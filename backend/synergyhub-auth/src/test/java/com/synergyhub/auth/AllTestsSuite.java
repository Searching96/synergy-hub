package com.synergyhub.auth;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("SynergyHub Auth Module - Complete Test Suite")
@SelectPackages({
        "com.synergyhub.auth.controller",
        "com.synergyhub.auth.service",
        "com.synergyhub.auth.security",
        "com.synergyhub.auth.util",
        "com.synergyhub.auth.entity",
        "com.synergyhub.auth.validation"
})
public class AllTestsSuite {
    // This class runs all tests in the specified packages
}