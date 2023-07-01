package com.example.vorspiel.docxContent.basic.style;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Holds all styling information for a document.
 * 
 * @since 0.0.1
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Style {
    
    @NotNull(message = "placing cannot be null.")
    private Placing placing;

    @NotNull(message = "fontSize cannot be null.")
    private Integer fontSize;

    @NotEmpty(message = "fontFamily cannot be empty or null.")
    private String fontFamily;

    @NotNull(message = "style attribute color cannot be null.")
    private Color color;

    @NotNull(message = "style attribute bold cannot be null.")
    private Boolean bold;

    @NotNull(message = "style attribute italic cannot be null.")
    private Boolean italic;

    @NotNull(message = "style attribute underline cannot be null.")
    private Boolean underline;

    @NotNull(message = "indentFirstLine cannot be null.")
    private Boolean indentFirstLine;

    @NotNull(message = "indentParagraph cannot be null.")
    private Boolean indentParagraph;

    @NotNull(message = "textAlign cannot be null.")
    private ParagraphAlignment textAlign;

    @NotNull(message = "pageBreak cannot be null.")
    private Boolean pageBreak;
}