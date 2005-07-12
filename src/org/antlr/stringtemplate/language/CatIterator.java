package org.antlr.stringtemplate.language;

import java.util.Iterator;

/** Given two iterators, return the combined elements one by one. */
public class CatIterator implements Iterator {
	Iterator a;
	Iterator b;
	public CatIterator(Iterator a, Iterator b) {
		this.a = a;
		this.b = b;
	}

	public boolean hasNext() {
		return a.hasNext() || b.hasNext();
	}

	public Object next() {
		if ( a.hasNext() ) {
			return a.next();
		}
		if ( b.hasNext() ) {
			return b.next();
		}
		return null;
	}

	public void remove() {
		throw new RuntimeException("unimplemented method: CatIterator remove()");
	}

	/** The result of asking for the string of an iterator is the list of elements
	 *  and so this is just the cat'd list of both elements
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		while (a.hasNext()) {
			Object o = (Object) a.next();
			buf.append(o.toString());
		}
		while (b.hasNext()) {
			Object o = (Object) b.next();
			buf.append(o.toString());
		}
		return buf.toString();
	}
}
