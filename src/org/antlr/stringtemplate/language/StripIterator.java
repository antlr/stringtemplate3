package org.antlr.stringtemplate.language;

import java.util.Iterator;

/** Given a list of iterators, return the combined elements one by one. */
public class StripIterator implements Iterator {
	/** List of iterators to cat together */
	protected Iterator it;
	/** To know if we hasNext() we need to see if it's null or not */
	protected Object lookahead;

	public StripIterator(Iterator it) {
		this.it = it;
		if ( it.hasNext() ) {
			consume(); // prime lookahead
		}
	}

	protected void consume() {
		do {
			lookahead = it.next();
		}
		while ( lookahead==null && it.hasNext() );
	}

	public boolean hasNext() {
		return it.hasNext() && lookahead!=null;
	}

	public Object next() {
		Object v = lookahead;
		consume();
		return v;
	}

	public void remove() {
		throw new RuntimeException("unimplemented method: StripIterator remove()");
	}

	/** The result of asking for the string of an iterator is the list of elements
	 *  and so this is just the list w/o nulls.  This is destructive
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
