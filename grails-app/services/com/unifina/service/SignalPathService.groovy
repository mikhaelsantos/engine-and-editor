package com.unifina.service

import com.google.gson.Gson
import com.unifina.api.CanvasCommunicationException
import com.unifina.datasource.IStartListener
import com.unifina.datasource.IStopListener
import com.unifina.domain.security.Permission
import com.unifina.domain.security.SecUser
import com.unifina.domain.signalpath.Canvas
import com.unifina.domain.signalpath.Serialization
import com.unifina.exceptions.CanvasUnreachableException
import com.unifina.serialization.SerializationException
import com.unifina.signalpath.*
import com.unifina.utils.Globals
import com.unifina.utils.GlobalsFactory
import com.unifina.utils.NetworkInterfaceUtils
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.security.AccessControlException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SignalPathService {

    static transactional = false
	
	def servletContext
	def grailsApplication
	def grailsLinkGenerator
	def serializationService
	StreamService streamService
	PermissionService permissionService
	CanvasService canvasService
	ApiService apiService

	private static final Logger log = Logger.getLogger(SignalPathService.class)

	/**
	 * Creates and configures a root SignalPath instance with the given config and Globals. You
	 * can pass an optional SignalPath instance to configure if you want (eg. to configure non-root
	 * SignalPaths or subclasses of SignalPath).
	 *
	 * If connectionsReady==true, instance.connectionsReady() is called.
     */
	@CompileStatic
	SignalPath mapToSignalPath(Map config, boolean connectionsReady, Globals globals, SignalPath instance = new SignalPath(true)) {
		instance.globals = globals
		instance.init()
		instance.configure(config)
		if (connectionsReady) {
			instance.connectionsReady()
		}

		return instance
	}

	@CompileStatic
	private static Map signalPathToMap(SignalPath sp) {
		return  [
			name: sp.name,
			modules: sp.modules.collect { AbstractSignalPathModule it -> it.getConfiguration() },
			settings: sp.globals.signalPathContext,
			hasExports: sp.hasExports(),
			uiChannel: sp.getUiChannel().toMap()
		]
	}
	
	/**
	 * Rebuilds a saved representation of a root SignalPath along with its config.
	 * Potentially modifies the config given as parameter.
	 */
	@CompileStatic
	Map reconstruct(Map config, Globals globals) {
		SignalPath sp = mapToSignalPath(config, true, globals, new SignalPath(true))
		return signalPathToMap(sp)
	}

	@Transactional
	void deleteReferences(SignalPath signalPath, boolean delayed) {
		canvasService.deleteCanvas(signalPath.canvas, SecUser.load(signalPath.globals.userId), delayed)
	}

	/**
	 * @throws SerializationException if de-serialization fails when resuming from existing state
     */
	void startLocal(Canvas canvas, Map signalPathContext, SecUser asUser) throws SerializationException {
		Globals globals = GlobalsFactory.createInstance(signalPathContext, asUser)
		SignalPath sp

		// Instantiate the SignalPath
		if (canvas.serialization == null || canvas.adhoc) {
			log.info("Creating new signalPath connections (canvasId=$canvas.id)")
			sp = mapToSignalPath(new Gson().fromJson(canvas.json, Map.class), false, globals, new SignalPath(true))
		} else {
			log.info("De-serializing existing signalPath (canvasId=$canvas.id)")
			sp = (SignalPath) serializationService.deserialize(canvas.serialization.bytes)
		}

		sp.canvas = canvas

		// Create the runner thread
		SignalPathRunner runner = new SignalPathRunner(sp, globals, canvas.adhoc)

		runner.addStartListener(new IStartListener() {
			@Override
			void onStart() {
				addRunner(runner)
			}
		})

		runner.addStopListener(new IStopListener() {
			@Override
			void onStop() {
				removeRunner(runner)
			}
		})

		String runnerId = runner.runnerId
		canvas.runner = runnerId

		def port = Holders.getConfig().streamr.cluster.internalPort
		def protocol = Holders.getConfig().streamr.cluster.internalProtocol
		canvas.server = NetworkInterfaceUtils.getIPAddress(grailsApplication.config.streamr.ip.address.prefixes ?: []).getHostAddress()

		// Form an internal url that Streamr nodes will use to directly address this machine and the canvas that runs on it
		canvas.requestUrl = protocol + "://" + canvas.server + ":" + port + grailsLinkGenerator.link(uri: "/api/v1/canvases/$canvas.id", absolute: false)
		canvas.state = Canvas.State.RUNNING

		canvas.save(flush: true)

		// Start the runner thread
		runner.start()

		// Wait for runner to be in running state
		runner.waitRunning(true)
		if (!runner.getRunning()) {
			if (runner.thrownOnStartUp) { // failed because of error
				throw runner.thrownOnStartUp
			} else {					  // failed because of timeout
				runner.abort()
				def msg = "Timed out while waiting for canvas $canvas.id to start."
				throw new CanvasCommunicationException(msg)
			}
		}
	}

	/**
	 * Get a mapping of all running canvases' ids as keys and the users who started them as values.
	 * @return canvasId => user
	 */
	@CompileStatic
	Map<String, SecUser> getUsersOfRunningCanvases() {
		Map<String, SecUser> canvasIdToUser = [:]
		runners().values().each { SignalPathRunner runner ->
			SecUser user = SecUser.loadViaJava(runner.globals.userId)
			runner.signalPaths.each { SignalPath sp ->
				canvasIdToUser[sp.canvas.id] = user
			}
		}
		return canvasIdToUser
	}

	@CompileStatic
	List<Canvas> stopAllLocalCanvases() {
		// Copy list to prevent ConcurrentModificationException
		Map<String, SignalPathRunner> copyOfRunners = [:]
		copyOfRunners.putAll(runners())
		List canvases = []
		copyOfRunners.each { String key, SignalPathRunner runner ->
			if (stopLocalRunner(key)) {
				canvases.addAll(runner.getSignalPaths()*.getCanvas())
			}
		}
		return canvases
	}

	boolean stopLocalRunner(String runnerId) {
		SignalPathRunner runner = getRunner(runnerId)
		if (runner != null) {
			runner.abort()

			// Wait for runner to be in stopped state
			runner.waitRunning(false)
			if (runner.getRunning()) {
				log.error("Timed out while waiting for runner $runnerId to stop!")
				return false
			} else {
				return true
			}
		} else {
			log.error("stopLocal: could not find runner $runnerId!")
			updateState(runnerId, Canvas.State.STOPPED)
			return false
		}
	}

	boolean stopLocal(Canvas canvas) {
		return stopLocalRunner(canvas.runner)
	}

	@NotTransactional
	@CompileStatic
	Map stopRemote(Canvas canvas, SecUser user) {
		return runtimeRequest(buildRuntimeRequest([type:"stopRequest"], "canvases/$canvas.id", user))
	}

	@CompileStatic
	boolean ping(Canvas canvas, SecUser user) {
		runtimeRequest(buildRuntimeRequest([type:'ping'], "canvases/$canvas.id", user))
		return true
	}

	@CompileStatic
	private Map sendRemoteRequest(RuntimeRequest req) {
		// Require the request to be local to the receiving server to avoid redirect loops in case of invalid data
		String url = req.getCanvas().getRequestUrl().replace("canvases/${req.getCanvas().id}", req.getOriginalPath() + "/request?local=true")
		return apiService.post(url, req, req.getUser().keys.iterator().next())
	}

	@CompileStatic
	RuntimeRequest buildRuntimeRequest(Map msg, String path, String originalPath = path, SecUser user) {
		RuntimeRequest.PathReader pathReader = RuntimeRequest.getPathReader(path)

		// All runtime requests require at least read permission
		Canvas canvas = canvasService.authorizedGetById(pathReader.readCanvasId(), user, Permission.Operation.READ)
		Set<Permission.Operation> checkedOperations = new HashSet<>()
		checkedOperations.add(Permission.Operation.READ)

		return new RuntimeRequest(msg, user, canvas, path, originalPath, checkedOperations)
	}

	@CompileStatic
	Map runtimeRequest(RuntimeRequest req, boolean localOnly = false) {
		SignalPathRunner spr = getRunner(req.getCanvas().runner)
		
		log.info("runtimeRequest: $req, path: ${req.getPath()}, localOnly: $localOnly")
		
		// Give an error if the runner was not found locally although it should have been
		if (localOnly && !spr) {
			log.error("runtimeRequest: $req, runner not found with localOnly=true, responding with error")
			throw new CanvasUnreachableException("Canvas does not appear to be running!")
		}
		// May be a remote runner, check server and send a message
		else if (!localOnly && !spr) {
			try {
				return sendRemoteRequest(req)
			} catch (Exception e) {
				log.error("Unable to contact remote Canvas id ${req.getCanvas().id} at ${req.getCanvas().requestUrl}")
				throw new CanvasUnreachableException("Unable to communicate with remote server!")
			}
		}
		// If runner found
		else {
			SignalPath sp = spr.signalPaths.find {SignalPath it->
				it.canvas.id == req.getCanvas().id
			}
			
			if (!sp) {
				log.error("runtimeRequest: $req, runner found but canvas not found. This should not happen. Canvas: ${req.canvas}, path: ${req.path}")
				throw new CanvasUnreachableException("Canvas not found in runner. This should not happen.")
			}
			else {
				/**
				 * Special handling for runner thread stop request
				 */
				if (req.type=="stopRequest") {
					if (!permissionService.canWrite(req.getUser(), req.getCanvas())) {
						throw new AccessControlException("stopRequest requires write permission!");
					}

					if (stopLocal(req.getCanvas())) {
						return req
					}
					else {
						throw new CanvasUnreachableException("Canvas could not be stopped.")
					}
				}
				/**
				 * Requests for SignalPaths and modules within them
				 */
				else {
					RuntimeRequest.PathReader pathReader = req.getPathReader()

					// Consume the already-processed parts of the path and double-sanity-check canvas id
					if (pathReader.readCanvasId() != req.getCanvas().getId()) {
						throw new IllegalStateException("Unexpected path: ${req.getPath()}")
					}

					Future<RuntimeResponse> future = sp.onRequest(req, pathReader)
					
					try {
						RuntimeResponse resp = future.get(30, TimeUnit.SECONDS)
						log.debug("runtimeRequest: responding with $resp")
						return resp
					} catch (TimeoutException e) {
						throw new CanvasUnreachableException("Timed out while waiting for response.")
					}
				}
				
			}
		}
		
	}
	
	void updateState(String runnerId, Canvas.State state) {
		Canvas.executeUpdate("update Canvas c set c.state = ? where c.runner = ?", [state, runnerId])
	}

	@Transactional
	def saveState(SignalPath sp) {
		long startTime = System.currentTimeMillis()
		Canvas canvas = Canvas.get(sp.canvas.id)

		try {
			boolean isFirst = (canvas.serialization == null)
			Serialization serialization = isFirst ? new Serialization(canvas: canvas) : canvas.serialization

			// Serialize
			byte[] bytes = serializationService.serialize(sp)
			boolean notTooBig = bytes.length <= serializationService.serializationMaxBytes()

			if (notTooBig) {
                serialization.bytes = serializationService.serialize(sp)
                serialization.date = sp.globals.time
                serialization.save(failOnError: true, flush: true)
                canvas.serialization = serialization

                if (isFirst) {
                    Canvas.executeUpdate("update Canvas c set c.serialization = ? where c.id = ?", [serialization, canvas.id])
                }
			}

			long timeTaken = System.currentTimeMillis() - startTime
			String stats = "(size: ${bytes.length} bytes, processing time: ${timeTaken} ms)"
			if (notTooBig) {
				log.info("Canvas " + canvas.id + " serialized " + stats)
			} else {
				log.info("Canvas " + canvas.id + " serialization skipped because too large " + stats)
			}
		} catch (SerializationException ex) {
			log.error("Serialization of canvas " + canvas.id + " failed.")
			throw ex
		} finally {
			// Save memory by removing reference to the bytes to get them gc'ed
			canvas.serialization?.bytes = null
		}
	}

	@Transactional
	def clearState(Canvas canvas) {
		canvas.serialization?.delete()
		canvas.serialization = null
		canvas.save(failOnError: true)
		log.info("Canvas $canvas.id serialized state cleared.")
	}

	@CompileStatic
	private Map<String, SignalPathRunner> runners() {
		(servletContext["signalPathRunners"]) as Map<String, SignalPathRunner>
	}

	@CompileStatic
	private SignalPathRunner getRunner(String runnerId) {
		runners().get(runnerId)
	}

	@CompileStatic
	private void addRunner(SignalPathRunner runner) {
		runners().put(runner.runnerId, runner)
	}

	@CompileStatic
	private void removeRunner(SignalPathRunner runner) {
		runners().remove(runner.runnerId)
	}
}
