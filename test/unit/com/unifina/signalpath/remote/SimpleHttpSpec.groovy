package com.unifina.signalpath.remote

import com.unifina.datasource.DataSource
import com.unifina.datasource.DataSourceEventQueue
import com.unifina.domain.signalpath.Canvas
import com.unifina.signalpath.SignalPath
import com.unifina.utils.Globals
import com.unifina.utils.testutils.ModuleTestHelper
import grails.test.mixin.Mock
import groovy.json.JsonBuilder
import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.StringEntity
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import spock.lang.Specification

@Mock(Canvas)
class SimpleHttpSpec extends Specification {
	SimpleHttp module

	/**
	 * Module input values for each iteration, and corresponding expected output values
	 * Maps  input/output name -> List of values
	 */
	Map<String, List> inputs, outputs

	/**
	 * Override "response" to provide the mock server implementation
	 * If closure, will be executed (argument is HttpUriRequest)
	 * If constant, will be returned
	 * If array, elements will be returned in sequence (closures executed, cyclically repeated if too short)
	 * If you want to return an array,
	 *   use closure that returns an array (see default below)
	 *   or array of arrays
	 */
	def response = { request -> [] }

	/**
	 * Init module for test
	 * @param inputs List of names, or number of inputs to be autogenerated (in1, in2, ...)
	 * @param outputs List of names, or number of outputs to be autogenerated (out1, out2, ...)
     */
	private void init(inputs=[], outputs=[], List headers=[], boolean async=true, List moreParams=[]) {
		if (inputs instanceof Integer) { inputs = (inputs < 1) ? [] : (1..inputs).collect { "in"+it } }
		if (outputs instanceof Integer) { outputs = (outputs < 1) ? [] : (1..outputs).collect { "out"+it } }
		isAsync = async

		// TestableSimpleHttp is SimpleHttp module wrapped so that we can inject our own mock HttpClient
		// Separate class is needed in same path as SimpleHttp.java; anonymous class won't work with de-serializer
		TestableSimpleHttp.httpClient = mockClient
		module = new TestableSimpleHttp()
		module.init()
		int i = 0, o = 0
		module.configure([
			options: [
				inputCount: [value: inputs.size()],
				outputCount: [value: outputs.size()],
				headerCount: [value: headers.size()],
				syncMode: [value: isAsync ? "async" : "sync"]
			],
			inputs: inputs.collect {[name: "in"+(++i), displayName: it]},
			outputs: outputs.collect {[name: "out"+(++o), displayName: it]},
			params: [
			    *headers,
				*moreParams
			]
		])
		def signalPath = new SignalPath(true)
		signalPath.setCanvas(new Canvas())
		module.setParentSignalPath(signalPath)
	}

	private boolean test() {
		return new ModuleTestHelper.Builder(module, inputs, outputs)
			.overrideGlobals { mockGlobals }
			.onModuleInstanceChange { newInstance -> module = newInstance }
			.test()
	}

	/** Mocked event queue. Works manually in tests, please call module.receive(queuedEvent) */
	def mockGlobals = Stub(Globals) {
		getDataSource() >> Stub(DataSource) {
			enqueueEvent(_) >> { feedEventList ->
				transaction = feedEventList[0].content
			}
		}
		isRealtime() >> true
	}

	// temporary storage for async transaction generated by AbstractHttpModule, passing from globals to mockClient
	AbstractHttpModule.HttpTransaction transaction
	boolean isAsync = true

	/** HttpClient that generates mock responses to HttpUriRequests according to this.response */
	def mockClient = Stub(HttpAsyncClient) {
		def responseI = [].iterator()
		execute(_, _) >> { HttpUriRequest request, FutureCallback<HttpResponse> future ->
			def mockHttpResponse = Stub(CloseableHttpResponse) {
				getEntity() >> {
					def ret = response
					// array => iterate
					if (ret instanceof Iterable) {
						// end of array -> restart from beginning
						if (!responseI.hasNext()) {
							responseI = response.iterator()
						}
						ret = responseI.hasNext() ? responseI.next() : []
					}
					// closure => execute
					if (ret instanceof Closure) {
						ret = ret(request)
					}
					// wrap mock response object in JSON and HttpEntity
					return new StringEntity(new JsonBuilder(ret).toString())
				}
			}
			// synchronized requests sendOutput here already
			future.completed(mockHttpResponse)

			// simulate AbstractHttpModule.receive, but without propagation
			if (isAsync) {
				module.sendOutput(transaction)
			}
		}
	}

	void "no input, no response, async"() {
		init()
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null]]
		expect:
		test()
	}

	void "no input, no response, synchronized"() {
		init([], [], [], false)
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null]]
		expect:
		test()
	}

	void "no input, unexpected object response (ignored)"() {
		init()
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null]]
		response = [foo: 3, bar: 2, shutdown: "now"]
		expect:
		test()
	}

	void "no input, object response"() {
		init(0, ["foo"])
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null], foo: [3, 3, 3]]
		response = [foo: 3]
		expect:
		test()
	}

	void "empty response"() {
		init(1, 1)
		inputs = [in1: [4, 20, "everyday"]]
		outputs = [errors: [null, null, null]]
		response = []
		expect:
		test()
	}

	void "one input, echo response (just value, no JSON object wrapper)"() {
		init(1, ["foo"])
		def messages = ["4", "20", "everyday"]
		inputs = [in1: messages]
		outputs = [errors: [null, null, null], foo: messages]
		response = { HttpPost r ->
			def jsonString = EntityUtils.toString(r.entity)
			def ob = new JSONObject(jsonString)
			return ob.get("in1")
		}
		expect:
		test()
	}

	void "two inputs, three outputs, array constant response with too many elements (ignored)"() {
		init(2, 3)
		inputs = [in1: [4, 20, "everyday"], in2: [1, 2, "ree"]]
		outputs = [errors: [null, null, null], out1: [true, true, true],
							out2 : ["developers", "developers", "developers"], out3: [1, 1, 1]]
		response = { request -> [true, "developers", 1, 2, 3, 4] }
		expect:
		test()
	}

	void "two inputs, three outputs, array varying response with too few elements"() {
		init(2, 3)
		inputs = [in1: [4, 20, "everyday"], in2: [1, 2, "ree"]]
		outputs = [errors: [null, null, null], out1: [":)", ":|", ":("]]
		response = [[":)"], [":|"], [":("]]
		expect:
		test()
	}

	void "two inputs, three outputs, array varying length response"() {
		init(2, 3)
		inputs = [in1: [4, 20, "everyday"], in2: [1, 2, "ree"]]
		outputs = [errors: [null, null, null], out1: [":)", ":|", ":("],
							out2 : [null, 8, 7], out3: [null, null, 6]]
		response = [[":)"], [":|", 8], [":(", 7, 6, 5, 4, 3]]
		expect:
		test()
	}

	void "GET request generates correct URL params"() {
		init(["inputput", "nother"], 0, [], false, [
			[name: "URL", value: "localhost"],
			[name: "verb", value: "GET"]
		])
		inputs = [inputput: [666, "666", 2 * 333], nother: [1 + 1 == 2, true, "true"]]
		outputs = [errors: [null, null, null]]
		response = { HttpUriRequest request ->
			assert request.URI.toString().equals("localhost?inputput=666&nother=true")
		}
		expect:
		test()
	}

	void "HTTP request headers are transmitted correctly"() {
		def headers = [
			user  : [name: "header1", displayName: "user", value: "head"],
			token : [name: "header2", displayName: "token", value: "bang"],
			apikey: [name: "header3", displayName: "apikey", value: "er"]
		]
		init(0, 0, headers.values().toList())
		inputs = [trigger: [1, true, "metal", 666]]
		outputs = [:]
		response = { HttpUriRequest request ->
			int found = 0
			request.allHeaders.each { Header h ->
				if (headers.containsKey(h.name)) {
					assert headers[h.name].value == h.value
					found++
				}
			}
			assert found == headers.size()
		}
		expect:
		test()
	}

	void "JSON object dot notation works for output parsing"() {
		init(0, ["seasons", "best.pony", "best.pals.human"])
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null], seasons: [4, 4, 4],
							"best.pony": ["Pink", "Pink", "Pink"], "best.pals.human": ["Finn", "Finn", "Finn"]]
		response = [best: [pony: "Pink", pals: [dog: "Jake", human: "Finn"]], seasons: 4]
		expect:
		test()
	}

	void "JSON object dot notation supports array indexing too"() {
		init(0, ["seasons[1]", "best.pals.count", "best.pals[1].name"])
		inputs = [trigger: [1, true, "test"]]
		outputs = [errors: [null, null, null], "seasons[1]": [3, 3, 3],
				   "best.pals.count": [2, 2, 2], "best.pals[1].name": ["Finn", "Finn", "Finn"]]
		response = [best: [pals: [[name: "Jake", species: "Dog"], [name: "Finn", species: "Human"]]], seasons: [4,3,2,1]]
		expect:
		test()
	}
}
