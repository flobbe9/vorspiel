package com.example.vorspiel.documentBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;
import com.example.vorspiel.documentParts.BasicParagraph;
import com.example.vorspiel.documentParts.TableConfig;
import com.example.vorspiel.documentParts.style.Style;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;


/**
 * Class to build and write a .docx document.
 * 
 * @since 0.0.1
 * @see BasicParagraph
 * @see Style
 */
@Log4j2
@Getter
@Setter
public class DocumentBuilder {

    public static final String RESOURCE_FOLDER = "./resources";

    /** paragraph indentation */
    public static final int INDENT_ONE_THIRD_PORTRAIT = 2000;

    /** table dimensions */
    public static final int PAGE_LONG_SIDE_WITH_BORDER = 13300;

    /** orientation dimensions  */
    public static final BigInteger PAGE_LONG_SIDE = BigInteger.valueOf(842 * 20);
    public static final BigInteger PAGE_SHORT_SIDE = BigInteger.valueOf(595 * 20);

    /** picture dimensions in centimeters. */
    public static final int PICTURE_WIDTH_PORTRAIT = 15;
    public static final int PICTURE_WIDTH_LANDSCAPE_HALF = 11;
    public static final int PICTURE_HEIGHT_LANDSCAPE_HALF = 7;

    /** document margins */
    public static final int MINIMUM_MARGIN_TOP_BOTTOM = 240;

    /** minimum line space (Zeilenabstand) */
    public static final int NO_LINE_SPACE = 1;
    
    @NotNull(message = "'content' cannot be null.")
    private List<BasicParagraph> content;
    
    @NotEmpty(message = "'docxFileName' cannot be empty or null.")
    @Pattern(regexp = ".*\\.docx$", message = "Wrong format of 'docxFileName'. Only '.dox' permitted.")
    private String docxFileName;

    private PictureUtils pictureUtils;

    private TableUtils tableUtils;  

    private XWPFDocument document;

    
    /**
     * Reading the an empty document from an existing file.<p>
     * 
     * Pictures may be added.
     * 
     * @param content list of {@link BasicParagraph}s
     * @param docxFileName file name to write the .docx file to
     * @param pictures list of files containing pictures
     * @see PictureType for allowed formats
     */
    public DocumentBuilder(List<BasicParagraph> content, String docxFileName, File... pictures) {

        this.content = content;
        this.docxFileName = docxFileName;
        this.pictureUtils = new PictureUtils(Arrays.asList(pictures));
        this.document = readDocxFile("EmptyDocument_2Columns.docx");
    }


    /**
     * Reading the an empty document from an existing file.<p>
     * 
     * Pictures and/or one table may be added.
     * 
     * @param content list of {@link BasicParagraph}s
     * @param docxFileName file name to write the .docx file to
     * @param tableConfig wrapper with configuration data for the table to insert
     * @param pictures list of files containing pictures
     * @see PictureType for allowed formats    
     */
    public DocumentBuilder(List<BasicParagraph> content, String docxFileName, TableConfig tableConfig, File... pictures) {

        this.content = content;
        this.docxFileName = docxFileName;
        this.pictureUtils = new PictureUtils(Arrays.asList(pictures));
        this.document = readDocxFile("EmptyDocument_2Columns.docx");
        this.tableUtils = new TableUtils(this.document, tableConfig);
    }


    /**
     * Builds a the document with given list of {@link BasicParagraph}s and writes it to a .docx file which will
     * be located in the {@link #RESOURCE_FOLDER}.
     * 
     * @return true if document was successfully written to a .docx file
     */
    public boolean build() {
        
        log.info("Starting to build document...");
        
        setOrientation(STPageOrientation.LANDSCAPE);

        addContent();
        
        setDocumentMargins(MINIMUM_MARGIN_TOP_BOTTOM, null, MINIMUM_MARGIN_TOP_BOTTOM, null);
        
        return writeDocxFile();
    }
    

    /**
     * Iterates {@link #content} list and adds all paragraphs to the document.
     */
    void addContent() {

        log.info("Adding content...");

        int numParagraphs = this.content.size();

        // case: no content
        if (numParagraphs == 0) 
            log.warn("Not adding any paragraphs because content list is empty.");

        for (int i = 0; i < numParagraphs; i++) 
            addParagraph(i);
    }


    /**
     * Adds {@link BasicParagraph} from content list at given index to the document. This includes text and style. <p>

     * If basicParagraph is null, an {@link XWPFPargraph} will be added anyway an hence appear as a line break. 
     * This applies <strong>not</strong> for header and footer.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     */
    void addParagraph(int currentContentIndex) {

        // get content
        BasicParagraph basicParagraph = getContent().get(currentContentIndex);
    
        XWPFParagraph paragraph = createParagraphByContentIndex(currentContentIndex);

        if (basicParagraph != null) {
            // add text
            addText(paragraph, basicParagraph, currentContentIndex);

            // add style
            addStyle(paragraph, basicParagraph.getStyle());
        }
    }


    /**
     * Adds an {@link XWPFParagraph} to the document either for the header, the footer or the main content. <p>
     * 
     * For the fist element (index = 0) a header paragraph will be generated and for the last element a footer paragraph.
     * Tables wont get a paragraph since it's generated in {@link TableUtils}. <p>
     * 
     * Any other element gets a normal paragraph.
     * 
     * @param currentContentIndex index of the {@link #content} element currently processed
     * @return created paragraph
     */
    XWPFParagraph createParagraphByContentIndex(int currentContentIndex) {

        // case: table does not need paragrahp from this method
        if (this.tableUtils != null && this.tableUtils.isTableIndex(currentContentIndex))
            return null;

        BasicParagraph basicParagraph = getContent().get(currentContentIndex);

        // case: header
        if (currentContentIndex == 0) {
            if (basicParagraph != null)
                return getDocument().createHeader(HeaderFooterType.DEFAULT).createParagraph();

            return null;
        }

        // case: footer
        if (currentContentIndex == this.getContent().size() - 1) {
            if (basicParagraph != null)
                return getDocument().createFooter(HeaderFooterType.DEFAULT).createParagraph();

            return null;
        }

        // case: any other
        return getDocument().createParagraph();
    }


    /**
     * Adds the "text" class variable of {@link BasicParagraph} to given {@link XWPFRun}. <p>
     * 
     * "text" will be added as plain string, as picture or inside a table.
     * 
     * @param paragraph to add text and style to
     * @param basicParagraph to use the text and style information from
     * @param currentContentIndex index of the {@link #content} element currently processed
     */
    void addText(XWPFParagraph paragraph, BasicParagraph basicParagraph, int currentContentIndex) {

        String text = basicParagraph.getText();

        // case: picture
        if (this.pictureUtils != null && this.pictureUtils.isPicture(text)) {
            this.pictureUtils.addPicture(paragraph.createRun(), text);
        
        // case: table cell
        } else if (this.tableUtils != null && this.tableUtils.isTableIndex(currentContentIndex)) {
            this.tableUtils.addTableCell(currentContentIndex, text, basicParagraph.getStyle());
            
        // case: plain text
        } else
            paragraph.createRun().setText(text);
    }


    /**
     * Add style to given {@link XWPFParagraph}. Is skipped if either paragraph or style are null.
     * 
     * @param paragraph to apply the style to
     * @param style information to use
     * @see Style
     */
    static void addStyle(XWPFParagraph paragraph, Style style) {

        if (paragraph == null || style == null)
            return;

        paragraph.getRuns().forEach(run -> {
            run.setFontSize(style.getFontSize());

            run.setFontFamily(style.getFontFamily());

            run.setColor(style.getColor().getRGB());

            run.setBold(style.getBold());

            run.setItalic(style.getItalic());

            if (style.getBreakType() != null) 
                run.addBreak(style.getBreakType());

            if (style.getUnderline()) 
                run.setUnderline(UnderlinePatterns.SINGLE);
        });

        if (style.getIndentFirstLine()) 
            paragraph.setIndentationFirstLine(INDENT_ONE_THIRD_PORTRAIT);

        if (style.getIndentParagraph()) 
            paragraph.setIndentFromLeft(INDENT_ONE_THIRD_PORTRAIT);

        paragraph.setAlignment(style.getTextAlign());

        paragraph.setSpacingAfter(NO_LINE_SPACE);
    }


    /**
     * Set margins for the whole document.<p>
     * 
     * If null value will be set.
     * 
     * @param top margin
     * @param right margin
     * @param bottom margin
     * @param left margin
     */
    private void setDocumentMargins(Integer top, Integer right, Integer bottom, Integer left) {

        CTSectPr sectPr = getDocument().getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();

        if (top != null) 
            pageMar.setTop(BigInteger.valueOf(top));

        if (right != null) 
            pageMar.setRight(BigInteger.valueOf(right));

        if (bottom != null) 
            pageMar.setBottom(BigInteger.valueOf(bottom));

        if (left != null) 
            pageMar.setLeft(BigInteger.valueOf(left));
    }


    /**
     * Possible values are landscape or portrait.
     * If called multiple times the last call will be the effectiv one.
     * 
     * @param orientation landscape or portrait
     */
    private void setOrientation(STPageOrientation.Enum orientation) {

        if (orientation == null)
            return;

        setPageSizeDimensions(orientation);
        getPageSz().setOrient(orientation);
    }


    /**
     * Set height and width of the CTPageSz according to given orientation(landscape or portrait).
     * 
     * @param orientation the page should have
     * @param pageSize CTPageSz object of page
     * @return altered pageSize
     */
    private CTPageSz setPageSizeDimensions(STPageOrientation.Enum orientation) {

        CTPageSz pageSize = getPageSz();

        // case: landscape
        if (orientation.equals(STPageOrientation.LANDSCAPE)) {
            pageSize.setW(PAGE_LONG_SIDE);
            pageSize.setH(PAGE_SHORT_SIDE);

        // case: portrait
        } else {
            pageSize.setW(PAGE_SHORT_SIDE);
            pageSize.setH(PAGE_LONG_SIDE);
        }

        return pageSize;
    }


    /**
     * Get existing {@link CTPageSz} or add new one.
     * 
     * @return pageSz object of document
     */
    private CTPageSz getPageSz() {

        CTSectPr sectPr = getSectPr();

        return sectPr.getPgSz() == null ? sectPr.addNewPgSz() : sectPr.getPgSz();
    }


    /**
     * Get existing {@link CTSectPr} or add new one.
     * 
     * @return sectPr object of document
     */
    private CTSectPr getSectPr() {

        CTBody ctBody = getDocument().getDocument().getBody();

        return ctBody.getSectPr() == null ? ctBody.addNewSectPr() : ctBody.getSectPr();
    }


    /**
     * Writes the {@link XWPFDocument} to a .docx file. Stores it in {@link #RESOURCE_FOLDER}.
     * 
     * @return true if conversion was successful
     */
    synchronized boolean writeDocxFile() {

        log.info("Writing .docx file...");

        try (OutputStream os = new FileOutputStream(RESOURCE_FOLDER + prependSlash(docxFileName))) {

            this.document.write(os);
            this.document.close();
            
            return true;

        } catch (IOException e) {
            log.error("Failed to write .docx file. Cause: ");
            e.printStackTrace();
        }

        return false;
    }


    /**
     * Reads given .docx file to an {@link XWPFDocument} and cleans up any content. <p>
     * 
     * File is expected to be located in {@link #RESOURCE_FOLDER}. <p>
     * 
     * Creates and returns a new document if exception is caught.
     * 
     * @param fileName name and suffix of the .docx file
     * @return XWPFDocument of the file or an empty one in case of exception
     */
    XWPFDocument readDocxFile(String fileName) {

        log.info("Starting to read .docx file...");

        try {
            fileName = prependSlash(fileName);

            XWPFDocument document = new XWPFDocument(new FileInputStream(RESOURCE_FOLDER + fileName));

            // clean up document
            document.removeBodyElement(0);

            return document;
        
        } catch (Exception e) {
            log.warn("Failed to read docx file. Returning an empty document instead. Cause: " + e.getMessage());
            e.printStackTrace();

            return new XWPFDocument();
        }
    }


    /**
     * Convert any .docx file to .pdf file and store in {@link #RESOURCE_FOLDER}.<p>

     * Thread safe, since accessessing existing files. <p>

     * Is threadsafe since it accesses an existing resource.
     * 
     * @param docxInputStream inputStream of .docx file
     * @param pdfFileName name and suffix of pdf file
     * @return true if conversion was successful
     */
    public static synchronized boolean convertDocxToPdf(InputStream docxInputStream, String pdfFileName) {
            
        log.info("Converting .docx to .pdf...");
        
        try (OutputStream os = new FileOutputStream(RESOURCE_FOLDER + prependSlash(pdfFileName))) {
            IConverter converter = LocalConverter.builder().build();
            
            converter.convert(docxInputStream)
                .as(DocumentType.DOCX)
                .to(os)
                .as(DocumentType.PDF)
                .execute();

            converter.shutDown();

            return true;
                
        } catch (Exception e) {
            log.error("Failed to convert .docx to .pdf. Cause: ");
            e.printStackTrace();
        }
        
        return false;
    }


    /**
     * Overloading {@link #convertDocxToPdf(InputStream, String)}.
     * 
     * @param docxFile
     * @param pdfFileName
     * @return
     */
    public static synchronized boolean convertDocxToPdf(File docxFile, String pdfFileName) {

        try {
            return convertDocxToPdf(new FileInputStream(docxFile), pdfFileName);

        } catch (IOException e) {
            log.error("Failed to convert .docx to .pdf. Cause: ");
            e.printStackTrace();

            return false;
        }
    }


    /**
     * Prepends a '/' to given String if there isn't already one.
     * 
     * @param str String to prepend the slash to
     * @return the altered (or not altered) string or "/" if given str is null
     */
    static String prependSlash(String str) {

        if (str == null || str.equals(""))
            return "/";

        return str.charAt(0) == '/' ? str : "/" + str;
    }
}