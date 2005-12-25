package org.antlr.stringtemplate;

/** When group files derive from another group, we have to know how to
 *  load that group and its supergroups.  This interface also knows how
 *  to load interfaces.
 */
public interface StringTemplateGroupLoader {
	/** Load the group called groupName from somewhere.  Return null
	 *  if no group is found.
	 */
	public StringTemplateGroup loadGroup(String groupName);

	/** Load the interface called interfaceName from somewhere.  Return null
	 *  if no interface is found.
	 */
	public StringTemplateGroupInterface loadInterface(String interfaceName);
}
