package com.lxdvs.voj;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Rate {

	@JsonProperty("limit")
	int limit;
	
	@JsonProperty("remaining")
	int remaining;
	
	@JsonProperty("reset")
	int reset;
}
