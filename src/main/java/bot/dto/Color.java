package bot.dto;

public enum Color {
	FIRE("火"), WATER("水"), WIND("風"), LIGHT("光"),DARKNESS("闇");

	private Color(String name) {
        this.name = name;
    }
	
	private final String name;
	public String getName() {
        return name;
    }
}
