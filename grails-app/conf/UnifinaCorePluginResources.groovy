modules = {
	"jquery-ui-touchpunch" {
		dependsOn 'jquery-ui'
		resource url:'js/touchpunch/jquery.ui.touch-punch.min.js', disposition: 'head'
		//		resource url:'js/touchpunch/jquery.ui.touch-punch.js', disposition: 'head'
	}
	"jquery.ui-contextmenu" {
		dependsOn 'jquery-ui'
		resource url:'js/jquery.ui-contextmenu/jquery.ui-contextmenu.js'
	}
	tablesorter {
		dependsOn 'jquery'
		resource url:'js/tablesorter/jquery.tablesorter.min.js'
	}
	highcharts {
		resource url:'js/highcharts-2.3.3/highcharts.src.js'
	}
	highstock {
		resource url:'js/highstock-1.3.9/js/highstock.js'
		resource url:'js/highstock-1.3.9/js/highcharts-more.js'
	}
	codemirror {
//		resource url:'js/codemirror-3.21/codemirror.js'
		resource url:'js/codemirror-3.21/codemirror-compressed.js'
		resource url:'js/codemirror-3.21/codemirror.css'
//		resource url:'js/codemirror/codemirror.js'
//		resource url:'js/codemirror/groovy.js'
//		resource url:'js/codemirror/codemirror.css'
	}
	superfish {
		dependsOn 'jquery'
		resource url:'js/superfish/js/superfish.min.js'
		resource url:'js/superfish/css/superfish.css'
		resource url:'js/superfish/js/supposition.js'
	}

	jsplumb {
		dependsOn 'jquery'
		dependsOn 'jquery-ui'
		resource url:'js/jsPlumb/jquery.jsPlumb-1.5.3.js'
	}
	jstree {
		dependsOn 'jquery'
		resource url:'js/jsTree/jquery.jstree.js'
		// If you change the theme, check SignalPathTagLib too
		resource url:'js/jsTree/themes/classic/style.css'
	}
	atmosphere {
		dependsOn 'jquery'
		resource url:'js/atmosphere/jquery.atmosphere.js'
	}
	hotkeys {
		dependsOn 'jquery'
		resource url:'js/hotkeys/jquery.hotkeys.js'
	}
	joyride {
		dependsOn 'jquery'
		resource url:'js/joyride-2.1/joyride-2.1.css'
		resource url:'js/joyride-2.1/modernizr.mq.js'
		resource url:'js/joyride-2.1/jquery.cookie.js'
		resource url:'js/joyride-2.1/jquery.joyride-2.1.js'
	}
	pnotify {
		dependsOn 'jquery, jquery-ui'
//		resource url:'js/pnotify-1.2.0/jquery.pnotify.min.js'
		resource url:'js/pnotify-1.2.0/jquery.pnotify.1.2.2-snapshot.js'
		resource url:'js/pnotify-1.2.0/jquery.pnotify.default.css'
	}
	chosen {
		dependsOn 'jquery'
		resource url:'js/chosen-1.0.0/chosen.jquery.min.js'
		resource url:'js/chosen-1.0.0/chosen.min.css'
		resource url:'js/chosen-1.0.0/chosen-sprite.png'
		resource url:'js/chosen-1.0.0/chosen-sprite@2x.png'
	}
	'signalpath-loadBrowser' {
		resource url:'css/signalPath/widgets/loadBrowser.css'
	}
	'signalpath-core' {
		dependsOn 'jsplumb, jstree, highstock, atmosphere, codemirror, tablesorter, chosen, jquery.ui-contextmenu, signalpath-loadBrowser'
		resource url:'js/signalPath/core/signalPath.js'
		resource url:'js/timezones/detect_timezone.js'
		resource url:'js/signalPath/generic/emptyModule.js'
		resource url:'js/signalPath/generic/genericModule.js'
		resource url:'js/signalPath/core/IOSwitch.js'
		resource url:'js/signalPath/core/Endpoint.js'
		resource url:'js/signalPath/core/Input.js'
		resource url:'js/signalPath/core/Parameter.js'
		resource url:'js/signalPath/core/Output.js'
		resource url:'js/signalPath/specific/chartModule.js'
		resource url:'css/signalPath/modules/chartModule.css'
		resource url:'js/signalPath/specific/gaugeModule.js'
		resource url:'js/signalPath/specific/customModule.js'
		resource url:'js/signalPath/specific/tableModule.js'
		resource url:'js/signalPath/specific/commentModule.js'
		resource url:'css/signalPath/modules/commentModule.css'
		resource url:'js/signalPath/specific/labelModule.js'
	}
	'signalpath-theme' {
		dependsOn 'signalpath-core'
		resource url:'css/signalPath/signalPath.css'
		
		resource url:'css/signalPath/themes/light/light.css'
		resource url:'css/signalPath/themes/light/light.js'
		resource url:'css/signalPath/themes/light/jquery-ui-theme/jquery-ui-1.10.3.custom.min.css'
		
//		resource url:'css/signalPath/themes/dark/dark.css'
//		resource url:'css/signalPath/themes/dark/dark.js'
//		resource url:'css/signalPath/themes/dark/jquery-ui-theme/jquery-ui.min.css'
//		resource url:'css/signalPath/themes/dark/jquery-ui-theme/jquery.ui.theme.css'
	}
	overrides {
		'jquery-ui' {
			resource id:'js', url:'js/jquery-ui-1.9.2/jquery-ui.js', nominify: true, disposition: 'head'
		}
	}
}