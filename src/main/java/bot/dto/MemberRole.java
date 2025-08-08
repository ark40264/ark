package bot.dto;

public enum MemberRole {
	LEADER("リーダー"), SUB_LEADER("サブリーダー"), MEMBER("メンバー");

	private MemberRole(String name) {
        this.name = name;
    }
	
	private final String name;
	public String getName() {
        return name;
    }
}
