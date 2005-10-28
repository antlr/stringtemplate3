package org.antlr.stringtemplate;

/** When group files derive from another group, we have to know how to
 *  load that group and its supergroups.
 */
public interface StringTemplateGroupLoader {
	/** Load the group called groupName from somewhere.  Return null
	 *  if no group is found.
	 */
	public StringTemplateGroup load(String groupName);
}
