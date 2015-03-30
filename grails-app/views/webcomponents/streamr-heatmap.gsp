<link rel="import" href="${createLink(uri:"/webcomponents/polymer.html", plugin:"unifina-core")}">

<r:require module="streamr-heatmap"/>

<r:layoutResources disposition="head"/>
<r:layoutResources disposition="defer"/>

<polymer-element name="streamr-heatmap" extends="streamr-widget" attributes="lifeTime fadeInTime fadeOutTime min max radius center zoom minZoom maxZoom">
	<template>
		<link rel="stylesheet" href="${r.resource(uri:'/js/leaflet-0.7.3/leaflet-0.7.3.css')}">
		<streamr-client id="client"></streamr-client>
		<div id="container" style="min-height:400px"></div>
	</template>
	
	<script>
		Polymer('streamr-heatmap', {
			publish: {
				// hint that center is an array
				center: []
			},
			ready: function() {
				var _this = this

				var _this = this

				this.getModuleJson(function(json) {
					var resendOptions = _this.getResendOptions(json)

					_this.map = new StreamrHeatMap(_this.$.container, {
						lifeTime: this.lifeTime,
						fadeInTime: this.fadeInTime,
						fadeOutTime: this.fadeOutTime,
						min: this.min,
						max: this.max,
						radius: this.radius,
						center: (this.center!=null && this.center.length===2 ? this.center : undefined),
						zoom: this.zoom,
						minZoom: this.minZoom,
						maxZoom: this.maxZoom
					})

					_this.subscribe(
						function(message) {
					    	_this.map.handleMessage(message)
					    },
					    resendOptions
					)
				})
			},
			centerChanged: function(oldValue, newValue) {
				this.map.setCenter(newValue)
			}
		});
	</script>
</polymer-element>