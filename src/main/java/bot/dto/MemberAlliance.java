package bot.dto;

public enum MemberAlliance {
	MITSU("蜜"), FANZA("FANZA"), NONE("無所属");

	private MemberAlliance(String name) {
        this.name = name;
    }
	
	private final String name;
	public String getName() {
        return name;
    }
}
