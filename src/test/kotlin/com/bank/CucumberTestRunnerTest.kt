package com.bank

import io.cucumber.junit.platform.engine.Cucumber
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite
import org.springframework.test.context.ActiveProfiles

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.publish.enabled", value = "true")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @ignore")
@ActiveProfiles("test")
class CucumberTestRunnerTest 