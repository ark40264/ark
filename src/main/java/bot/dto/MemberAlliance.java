package bot.dto;

public enum MemberAlliance {
	HOKKORI("ほっこり茶屋"), HONTO_HOKKORI("本当にほっこり茶屋"), NONE("無所属");

	private MemberAlliance(String name) {
        this.name = name;
    }
	
	private final String name;
	public String getName() {
        return name;
    }
}
