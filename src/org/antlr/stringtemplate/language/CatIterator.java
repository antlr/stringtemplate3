package org.antlr.stringtemplate.language;

import java.util.Iterator;
import java.util.List;

/** Given a list of iterators, return the combined elements one by one. */
public class CatIterator implements Iterator {
	/** List of iterators to cat together */
	protected List iterators;

	public CatIterator(List iterators) {
		this.iterators = iterators;
	}

	public boolean hasNext() {
		for (int i = 0; i < iterators.size(); i++) {
			Iterator it = (Iterator) iterators.get(i);
			if ( it.hasNext() ) {
				return true;
			}
		}
		return false;
	}

	public Object next() {
		for (int i = 0; i < iterators.size(); i++) {
			Iterator it = (Iterator) iterators.get(i);
			if ( it.hasNext() ) {
				return it.next();
			}
		}
		return null;
	}

	public void remove() {
		throw new RuntimeException("unimplemented method: CatIterator remove()");
	}

	/** The result of asking for the string of an iterator is the list of elements
	 *  and so this is just the cat'd list of both elements.  This is destructive
	 *  in that the iterator cursors have moved to the end after printing.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		while ( this.hasNext() ) {
			buf.append(this.next());
		}
		return buf.toString();
	}
}
