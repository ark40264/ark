package bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Data;

@Data
@Entity
public class AllianceMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String memberRole;
	private String discordMemberId;
	private String discordName;
	private String ayarabuId;
	private String ayarabuName;
	private String alliance;
	private Integer statementCount;
	private String createDate;
	private Integer bot;
}
