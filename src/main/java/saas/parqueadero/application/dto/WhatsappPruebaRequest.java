package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappPruebaRequest {
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "El telefono debe estar en formato E.164 (ej: +573001234567)")
    private String telefono;
}
