package com.example.vorspiel.documentBuilder;

import static com.example.vorspiel.documentBuilder.DocumentBuilder.INDENT_ONE_THIRD_PORTRAIT;
import static com.example.vorspiel.documentBuilder.DocumentBuilder.RESOURCE_FOLDER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.example.vorspiel.documentParts.BasicParagraph;
import com.example.vorspiel.documentParts.TableConfig;
import com.example.vorspiel.documentParts.style.Style;
import com.example.vorspiel.documentParts.style.Color;


/**
 * Unit tests for {@link DocumentBuilder}.
 * 
 * @since 0.0.1
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DocumentBuilderTest {

    public static final String TEST_RESOURCE_FOLDER = "./src/test/java/com/example/vorspiel/testResources";

    private XWPFDocument document;

    private String testDocxFileName;

    private String docxFileName;
    
    private Style style;
    
    private BasicParagraph header; 
    private BasicParagraph title; 
    private BasicParagraph tableCell;
    private BasicParagraph picture;
    private BasicParagraph footer; 
    
    private List<BasicParagraph> content;

    private TableConfig tableConfig;
    private int numColumns;
    private int startIndex;
    private int endIndex;

    private TableUtils tableUtils;

    private PictureUtils pictureUtils;
    private String testPictureName;

    private DocumentBuilder documentBuilder;


    @BeforeEach
    void setup() {

        // picture
        this.testPictureName = "test.png";
        this.pictureUtils = new PictureUtils(Arrays.asList(new File(DocumentBuilderTest.TEST_RESOURCE_FOLDER + "/" + this.testPictureName)));

        // content
        this.style = new Style(11, 
                                    "times new roman", 
                                    Color.BLUE, 
                                    true, 
                                    true, 
                                    true,
                                    false,
                                    false,
                                    ParagraphAlignment.CENTER, 
                                    null);        
        this.header = new BasicParagraph("This is the header", this.style);
        this.title = new BasicParagraph("This is the title", this.style);
        this.tableCell = new BasicParagraph("This is a table cell", this.style);
        this.picture = new BasicParagraph(testPictureName, style);
        this.footer = new BasicParagraph("This is the footer", this.style);
        this.content = Arrays.asList(this.header, this.title, this.tableCell, this.picture, this.footer);
        
        // table
        this.numColumns = 3;
        this.startIndex = 2;
        this.endIndex = 2;
        this.tableConfig = new TableConfig(this.numColumns, this.numColumns, this.startIndex, this.endIndex);
        
        // document
        this.testDocxFileName = "test.docx";
        this.docxFileName = "basicTest.docx";
        this.documentBuilder = new DocumentBuilder(this.content, this.docxFileName, this.tableConfig, this.pictureUtils.getPictures().get(0));
        this.document = this.documentBuilder.getDocument();

        this.tableUtils = new TableUtils(this.document, this.tableConfig);  

        new File(RESOURCE_FOLDER + "/" + this.docxFileName).delete();
    }


//----------- build()
    @Test
    void build_paragarphAndFileShouldExist() {

        // should have no paragraphs
        assertTrue(this.document.getParagraphs().size() == 0);

        // file should not exist
        assertFalse(new File(DocumentBuilder.RESOURCE_FOLDER + "/" + this.docxFileName).exists());

        this.documentBuilder.build();

        // should have number of paragraphs minus header, footer and table
        assertEquals(this.content.size() - 3, this.document.getParagraphs().size());        

        // should have written to file
        assertTrue(new File(DocumentBuilder.RESOURCE_FOLDER + "/" + this.docxFileName).exists());
    }


//----------- addContent()
    @Test
    void addContent_shouldHaveNoParagraphWithoutContent() {

        // claer content
        this.documentBuilder.setContent(new ArrayList<>());

        // addContent
        this.documentBuilder.addContent();

        // expect num paragraphs 0
        assertEquals(0, this.document.getParagraphs().size());
    }


    @Test
    void addContent_shouldHaveParagraph() {

        // should have no paragraphs
        assertEquals(0, this.document.getParagraphs().size());

        this.documentBuilder.addContent();

        // should have number of paragraphs minus header, footer and table
        assertEquals(this.content.size() - 3, this.document.getParagraphs().size());
    }


//----------- addParagraph()
    @Test
    void addParagraph_basicParagraphNull_shouldNotThrow_shouldAddParagraph() {

        int currentContentIndex = this.content.indexOf(this.title);

        // should have no paragraphs
        assertTrue(this.document.getParagraphs().size() == 0);

        // set basicParagraph null
        this.content.set(currentContentIndex, null);
        assertDoesNotThrow(() -> this.documentBuilder.addParagraph(currentContentIndex));

        // should still have created paragraph
        assertTrue(this.document.getParagraphs().size() == 1);
    }


    @Test
    void addParagraph_shouldAddTextAndStyle() {

        // use title
        int currentContentIndex = this.content.indexOf(this.title);

        this.documentBuilder.addParagraph(currentContentIndex);

        BasicParagraph expectedParagraph = this.content.get(currentContentIndex);
        XWPFParagraph actualParagraph = this.document.getLastParagraph();
        
        // should have text
        assertEquals(expectedParagraph.getText(), actualParagraph.getText());

        // should have style, check one attribute
        assertEquals(expectedParagraph.getStyle().getTextAlign(), actualParagraph.getAlignment());
    }


//----------- createParagraphByContentIndex()
    @Test
    void createParagraphByContentIndex_isTableIndex_shouldReturnNull() {

        // use tableIndex
        int tableIndex = this.startIndex;

        // expect null
        assertNull(this.documentBuilder.createParagraphByContentIndex(tableIndex));

        // no tableIndex
        int titleIndex = this.content.indexOf(this.title);

        // expect no null
        assertFalse(this.documentBuilder.createParagraphByContentIndex(titleIndex) == null);
    }


    @Test
    void createParagraphByContentIndex_basicParagraphNull_shouldReturnNull() {

        // header
        this.content.set(0, null);
        assertNull(this.documentBuilder.createParagraphByContentIndex(0));

        // footer
        int endIndex = this.content.size() - 1;
        this.content.set(endIndex, null);
        assertNull(this.documentBuilder.createParagraphByContentIndex(endIndex));
    }

    
    @Test
    void createParagraphByContentIndex_shouldAddHeaderParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs());

        this.documentBuilder.createParagraphByContentIndex(0);
        assertTrue(this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultHeader().getParagraphs().size() == 1);
    }


    @Test
    void createParagraphByContentIndex_shouldAddFooterParagraph() {

        // should not have header yet
        assertThrows(NullPointerException.class, () -> this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs());

        this.documentBuilder.createParagraphByContentIndex(this.content.size() - 1);
        assertTrue(this.documentBuilder.getDocument().getHeaderFooterPolicy().getDefaultFooter().getParagraphs().size() == 1);
    }


    @Test
    void createParagraphByContentIndex_shouldReturnNormalParagraph() {

        // should not have paragraphs yet
        assertTrue(this.documentBuilder.getDocument().getParagraphs().size() == 0);

        this.documentBuilder.createParagraphByContentIndex(1);
        assertTrue(this.documentBuilder.getDocument().getParagraphs().size() == 1);
    }


//----------- addText() 
    @Test
    void addText_pictureUtilsNull_shouldNotAddPicture() {

        int pictureIndex = this.content.indexOf(this.picture);

        // set pictureUtils null
        this.documentBuilder.setPictureUtils(null);

        this.documentBuilder.addText(this.document.createParagraph(), this.picture, pictureIndex);
        
        // expect no picture
        assertEquals(0, this.document.getAllPictures().size());
    }


    @Test
    void addText_notAPicture_shouldNotAddPicture() {

        // use wrong index
        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(this.document.createParagraph(), this.title, titleIndex);

        // expect no picture
        assertEquals(0, this.document.getAllPictures().size());
    }

    
    @Test
    void addText_shouldAddPicture() {

        int pictureIndex = this.content.indexOf(this.picture);

        this.documentBuilder.addText(this.document.createParagraph(), this.picture, pictureIndex);

        // expect picture
        assertEquals(1, this.document.getAllPictures().size());
    }


    @Test
    void addText_tableUtilsNull_shouldNotAddCell() {

        int tableCellIndex = this.content.indexOf(this.tableCell);

        // set tableUtils null
        this.documentBuilder.setTableUtils(null);

        this.documentBuilder.addText(this.document.createParagraph(), this.tableCell, tableCellIndex);
        
        // expect no table
        assertEquals(0, this.document.getTables().size());        
    }


    @Test
    void addText_notATable_shouldNotAddTable() {

        // use wrong index
        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(this.document.createParagraph(), this.title, titleIndex);

        // expect no table
        assertEquals(0, this.document.getAllPictures().size());
    }


    @Test
    void addText_shouldAddTableCell() {

        int tableCellIndex = this.content.indexOf(this.tableCell);

        this.documentBuilder.addText(this.document.createParagraph(), this.tableCell, tableCellIndex);

        // expect table
        assertEquals(1, this.document.getTables().size());

        // expect tableCell text
        assertEquals(this.tableCell.getText(), this.document.getTables().get(0).getRow(0).getCell(0).getText());
    }


    @Test
    void addText_shouldAddText_shouldNotHaveTableOrPicture() {

        int titleIndex = this.content.indexOf(this.title);

        this.documentBuilder.addText(document.createParagraph(), this.title, titleIndex);

        // expect no table
        assertEquals(0, this.document.getAllPictures().size());

        // expect no picture
        assertEquals(0, this.document.getAllPictures().size());

        // expect title text
        assertEquals(this.title.getText(), this.document.getLastParagraph().getText());
    }


//----------- addStyle()
    @Test
    void addStyle_paragraphNull_shouldNotThrow() {

        assertDoesNotThrow(() -> DocumentBuilder.addStyle(null, this.style));
    }


    @Test
    void addStyle_styleNull_shouldNotThrow() {

        assertDoesNotThrow(() -> DocumentBuilder.addStyle(this.document.createParagraph(), null));
    }


    @Test
    void addStyle_shouldApplyStyleCorrectly() {

        // add some test content
        XWPFParagraph paragraph = this.document.createParagraph();
        XWPFRun run = paragraph.createRun();
        DocumentBuilder.addStyle(paragraph, this.style);

        // check each style attribute
        assertEquals(this.style.getFontSize(), (int) Math.round(run.getFontSizeAsDouble()));
        assertEquals(this.style.getFontFamily(), run.getFontFamily());
        assertEquals(this.style.getColor().getRGB(), run.getColor());
        assertEquals(this.style.getBold(), run.isBold());
        assertEquals(this.style.getItalic(), run.isItalic());
        assertEquals(this.style.getUnderline(), run.getUnderline().equals(UnderlinePatterns.SINGLE));
        assertEquals(this.style.getTextAlign(), paragraph.getAlignment());
    }


    @Test
    void addStyle_souldApplyIndentFirstLineCorrectly() {

        XWPFParagraph paragraph = this.document.createParagraph();

        this.style.setIndentFirstLine(false);
        DocumentBuilder.addStyle(paragraph, style);
        assertTrue(paragraph.getIndentationFirstLine() == -1);

        this.style.setIndentFirstLine(true);
        DocumentBuilder.addStyle(paragraph, style);
        assertTrue(paragraph.getIndentationFirstLine() == INDENT_ONE_THIRD_PORTRAIT);
    }


    @Test
    void addStyle_souldApplyIndentParagraphCorrectly() {

        XWPFParagraph paragraph = this.document.createParagraph();

        this.style.setIndentParagraph(false);
        DocumentBuilder.addStyle(paragraph, style);
        assertTrue(paragraph.getIndentFromLeft() == -1);

        this.style.setIndentParagraph(true);
        DocumentBuilder.addStyle(paragraph, style);
        assertTrue(paragraph.getIndentFromLeft() == INDENT_ONE_THIRD_PORTRAIT);
    }


    @Test 
    void addStyle_breakTypeNull_shouldNotThrow() {

        this.style.setBreakType(null);
        assertDoesNotThrow(() -> DocumentBuilder.addStyle(this.document.createParagraph(), this.style));
    }


//----------- writeDocxFile()
    @Test
    void writeToDocxFile_fileNameWithoutSlash_shouldBeTrue() {

        // file should not exist yet
        assertFalse(new File(RESOURCE_FOLDER + "/" + this.docxFileName).exists());

        // should return true
        assertTrue(this.documentBuilder.writeDocxFile());

        // file should exist
        assertTrue(new File(RESOURCE_FOLDER + "/" + this.docxFileName).exists());
    }


    @Test
    void writeToDocxFile_fileNameWithSlash_shouldBeTrue() {

        // file should not exist yet
        assertFalse(new File(RESOURCE_FOLDER + "/" + this.docxFileName).exists());

        this.documentBuilder.setDocxFileName("/" + this.docxFileName);
        assertTrue(this.documentBuilder.writeDocxFile());

        // file should exist
        assertTrue(new File(RESOURCE_FOLDER + "/" + this.docxFileName).exists());
    }


// ---------- readDocxFile()
    @Test
    void readDocxFile_shouldWorkWithFalsyInput() {

        XWPFDocument document = this.documentBuilder.readDocxFile("some non existing file");

        // expect clean document
        assertEquals(0, document.getParagraphs().size());
    }


    @Test
    void readDocxFile_shouldWorkWithTruthyInput() {

        XWPFDocument document = this.documentBuilder.readDocxFile(this.testDocxFileName);

        // expect clean document
        assertEquals(0, document.getParagraphs().size());
    }


//----------- prependSlash()
    @Test
    void prependSlash_strNull_shouldReturnSlash() {
        
        assertEquals("/", DocumentBuilder.prependSlash(null));
    }


    @Test
    void prependSlash_emptyStr_shouldReturnSlash() {

        assertEquals("/", DocumentBuilder.prependSlash(""));
    }


    @Test 
    void prependSlash_strWithSlash_shouldReturnSameStr() {

        assertEquals("/" + this.testDocxFileName, DocumentBuilder.prependSlash("/" + this.testDocxFileName));
    }


    @Test 
    void prependSlash_strWithoutSlash_shouldReturnStrWithSlash() {

        assertEquals("/" + this.testDocxFileName, DocumentBuilder.prependSlash(this.testDocxFileName));
    }


    @AfterAll
    void cleanUp() throws IOException {

        this.document.close();

        new File(RESOURCE_FOLDER + "/" + this.docxFileName).delete();
    }
}