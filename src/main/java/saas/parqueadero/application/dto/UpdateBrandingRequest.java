package saas.parqueadero.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBrandingRequest {

    /** Color principal en formato #RRGGBB; null o vacio vuelve al color por defecto. */
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener formato #RRGGBB")
    private String colorMarca;

    /** Imagen como data URL (png/jpeg/webp) ya reducida en el cliente; null o vacio quita el logo. */
    @Size(max = 400_000, message = "El logo es demasiado grande")
    @Pattern(regexp = "^(data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+)?$", message = "El logo debe ser una imagen png, jpeg o webp")
    private String logo;
}
