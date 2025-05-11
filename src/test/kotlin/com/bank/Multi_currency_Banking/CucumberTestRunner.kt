package com.bank.Multi_currency_Banking

import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.ConfigurationParameter

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.publish.enabled", value = "true")
class CucumberTestRunner 