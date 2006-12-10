package org.antlr.stringtemplate.language;

import java.util.Iterator;

/** Given an iterator, return only the non-null elements via next(). */
public class StripIterator implements Iterator {
	protected Iterator it;

	/** To know if stripped iterator hasNext(), we need to see if there
	 *  is another non-null element or not.
	 */
	protected Object lookahead;

	public StripIterator(Iterator it) {
		this.it = it;
		consume(); // prime lookahead
	}

	/** Set lookahead to next non-null element or null if nothing left */
	protected void consume() {
		if ( !it.hasNext() ) {
			lookahead = null;
			return;
		}
		Object e = null;
		// scan for next non-null value
		while ( e==null && it.hasNext() ) {
			e = it.next();
		}
		lookahead = e;
	}

	/** Either the list has more stuff or our lookahead has last element */
	public boolean hasNext() {
		return lookahead!=null;
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
