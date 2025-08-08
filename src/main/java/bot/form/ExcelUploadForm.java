package bot.form;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ExcelUploadForm {
	private MultipartFile[] multipartFiles;
}
