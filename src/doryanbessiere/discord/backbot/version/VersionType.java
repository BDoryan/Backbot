package doryanbessiere.discord.backbot.version;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public enum VersionType {

	SNAPSHOT("snapshot", "releases"),
	RELEASE("release", "releases");
	
	private String name;
	private String table_sql;
	
	private VersionType(String name, String table_sql) {
		this.name = name;
		this.table_sql = table_sql;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the table_sql
	 */
	public String getTableSQL() {
		return table_sql;
	}

	/**
	 * @param string
	 * @return
	 */
	public static VersionType from(String name) {
		for(VersionType versionType : VersionType.values()) {
			if(versionType.getName().equalsIgnoreCase(name))
				return versionType;
		}
		return null;
	}
}
