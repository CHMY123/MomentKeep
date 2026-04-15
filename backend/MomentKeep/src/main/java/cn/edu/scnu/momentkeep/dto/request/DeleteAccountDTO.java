package cn.edu.scnu.momentkeep.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteAccountDTO {
    
    @NotNull(message = "确认注销不能为空")
    private Boolean confirmation;
}
