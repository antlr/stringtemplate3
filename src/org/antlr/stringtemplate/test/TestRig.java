/*
 [The "BSD licence"]
 Copyright (c) 2003-2005 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.antlr.stringtemplate.test;

import java.lang.reflect.Method;

/** Terence's own version of a test case.  Got sick of trying to figure out
 *  the quirks of junit...this is pretty much the same functionality and
 *  only took me a few minutes to write.  Only missing the gui I guess from junit.
 *
 *  This class is the main testing rig.  It executes all the tests within a
 *  TestSuite.  Invoke like this:
 *
 *  $ java org.antlr.test.unit.TestRig yourTestSuiteClassName {any-testMethod}
 *
 *  $ java org.antlr.test.unit.TestRig org.antlr.test.TestIntervalSet
 *
 *  $ java org.antlr.test.unit.TestRig org.antlr.test.TestIntervalSet testNotSet
 *
 *  Another benefit to building my own test rig is that users of ANTLR or any
 *  of my other software don't have to download yet another package to make
 *  this code work.  Reducing library dependencies is good.  Also, I can make
 *  this TestRig specific to my needs, hence, making me more productive.
 */
public class TestRig {
    protected Class testCaseClass = null;

    /** Testing program */
    public static void main(String[] args) throws Exception {
        if ( args.length==0 ) {
            System.err.println("Please pass in a test to run; must be class with runTests() method");
        }
        String className = args[0];
        TestSuite test = null;
        try {
            Class c;
            try {
                c = Class.forName(className);
                test = (TestSuite)c.newInstance();
            }
            catch (Exception e) {
                System.out.println("Cannot load class: "+className);
                e.printStackTrace();
                return;
            }
			if ( args.length>1 ) {
				// run the specific test
				String testName = args[1];
				test.runTest(testName);
			}
            else {
				// run them all
                // if they define a runTests, just call it
                Method m;
                try {
                    m = c.getMethod("runTests",null);
                    m.invoke(test, null);
                }
                catch (NoSuchMethodException nsme) {
                    // else just call runTest on all methods with "test" prefix
					runAllTests(c, test);
				}
			}
        }
        catch (Exception e) {
            System.out.println("Exception during test "+test.testName);
            e.printStackTrace();
        }
        System.out.println();
        System.out.println("successes: "+test.getSuccesses());
        System.out.println("failures: "+test.getFailures());
    }

	public static void runAllTests(Class c, TestSuite test) {
		Method methods[] = c.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method testMethod = methods[i];
			if ( testMethod.getName().startsWith("test") ) {
				test.runTest(testMethod.getName());
			}
		}
	}
}
