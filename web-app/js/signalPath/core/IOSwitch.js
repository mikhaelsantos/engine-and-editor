SignalPath.IOSwitch = function(parentContainer, clazz, options) {
	var pub = pub || {};
	
	pub.div = $("<div class='ioSwitch'></div>");
	pub.parentContainer = parentContainer;
	
	pub.defaultOptions = {
		parentContainer: parentContainer,
		getValue: function() { alert("getValue not defined"); },
		setValue: function(value) { alert("setValue not defined"); },
		buttonText: function(currentValue) { return ""+currentValue; },
		nextValue: function(currentValue) { return !currentValue; },
		isActiveValue: function(currentValue) { return currentValue; },
		stateText: function() {
			var val = pub.getValue();
			if (val===true)
				return "on";
			else if (val===false || val==null)
				return "off";
			else return pub.getValue(); 
		},
		click: function() {
			var currentValue = pub.getValue();
			pub.setValue(pub.nextValue(currentValue));
			pub.update();
			pub.div.html(pub.buttonText());
		},
		update: function() {
			pub.div.html(pub.buttonText());
			if (pub.isActiveValue(pub.getValue())) {
				pub.div.addClass("ioSwitchTrue");
				pub.div.removeClass("ioSwitchFalse");
			}
			else {
				pub.div.removeClass("ioSwitchTrue");
				pub.div.addClass("ioSwitchFalse");
			}
			// The value may be visible elsewhere (eg. a tooltip), update that too
			$("#"+pub.getStateTextId()).html(pub.stateText());
		},
	}
	
	$.extend(pub, pub.defaultOptions, options);

	pub.getStateTextId = function() {
		if (!pub.stateTextId)
			pub.stateTextId = "ioSS_"+new Date().getTime();
		return pub.stateTextId;
	}
	
	pub.update();
	pub.div.data("spObject",pub);
	pub.div.click(function() {
		pub.click();
	});
	
	if (clazz)
		pub.div.addClass(clazz);
	
	pub.parentContainer.append(pub.div);
	return pub;
}