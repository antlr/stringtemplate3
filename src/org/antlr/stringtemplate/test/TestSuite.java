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
import java.lang.reflect.InvocationTargetException;

/** Terence's own version of a test case.  Got sick of trying to figure out
 *  the quirks of junit...this is pretty much the same functionality and
 *  only took me a few minutes to write.  Only missing the gui I guess from junit.
 */
public abstract class TestSuite {
    public String testName = null;

    int failures = 0, successes=0;

	public void assertEqual(Object result, Object expecting) throws FailedAssertionException {
		if ( result==null && expecting!=null ) {
			throw new FailedAssertionException("expecting \""+expecting+"\"; found null");
		}
		assertTrue(result.equals(expecting), "expecting \""+expecting+"\"; found \""+result+"\"");
	}

	public void assertEqual(int result, int expecting) throws FailedAssertionException {
		assertTrue(result==expecting,
				"expecting \""+expecting+"\"; found \""+result+"\"");
	}

    public void assertTrue(boolean test) throws FailedAssertionException {
        if ( !test ) {
            throw new FailedAssertionException();
        }
    }

	public void assertTrue(boolean test, String message) throws FailedAssertionException {
		if ( !test ) {
			if ( message!=null ) {
				throw new FailedAssertionException(message);
			}
			else {
				throw new FailedAssertionException("assertTrue failed");
			}
		}
	}

	public void time(String name, int n) throws Throwable {
		System.gc();
		long start = System.currentTimeMillis();
		System.out.print("TIME: "+name);
		for (int i=1; i<=n; i++) {
			invokeTest(name);
		}
		long finish = System.currentTimeMillis();
		long t = (finish-start);
		System.out.println("; n="+n+" "+t+"ms ("+((((double)t)/n)*1000.0)+" microsec/eval)");
	}

	public void runTest(String name) {
		try {
			System.out.println("TEST: "+name);
			invokeTest(name);
			successes++;
		}
		catch (InvocationTargetException ite) {
			failures++;
			try {
				throw ite.getCause();
			}
			catch (FailedAssertionException fae) {
				System.err.println(name+" failed: "+fae.getMessage());
			}
			catch (Throwable e) {
				System.err.print("exception during test "+name+":");
				e.printStackTrace();
			}
		}
	}

	public void invokeTest(String name)
			throws InvocationTargetException
	{
		testName = name;
		try {
			Class c = this.getClass();
			Method m = c.getMethod(name,null);
			m.invoke(this,null);
		}
		catch (IllegalAccessException iae) {
			System.err.println("no permission to exec test "+name);
		}
		catch (NoSuchMethodException nsme) {
			System.err.println("no such test "+name);
		}
	}

	public int getFailures() {
		return failures;
	}

	public int getSuccesses() {
		return successes;
	}

}
