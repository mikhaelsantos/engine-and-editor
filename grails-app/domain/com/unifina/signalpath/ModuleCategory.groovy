package com.unifina.signalpath

class ModuleCategory implements Comparable {
	String name
	int sortOrder
	ModulePackage modulePackage
	
	SortedSet modules
	SortedSet subcategories
	
	static hasMany = [modules:Module, subcategories:ModuleCategory]
	
	static belongsTo = [parent:ModuleCategory]
	
	static constraints = {
		parent(nullable:true)
		modulePackage(nullable:true)
	}
	
	int compareTo(o) {
		return this.sortOrder.compareTo(o.sortOrder)
	}
	
}
