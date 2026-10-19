package dev.pimon.listillo.ticket.lectura;

import dev.pimon.common.exception.AppException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/** El texto de un ticket digital en PDF, línea a línea en el orden en que se ve. */
public final class TextoDePdf {

    /** Un ticket ocupa una o dos páginas; más es otra cosa. */
    private static final int MAX_PAGINAS = 5;

    private TextoDePdf() {
    }

    public static String extraer(byte[] pdf) {
        if (pdf.length < 5 || !new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw AppException.badRequest("Eso no es un PDF. Sube el ticket digital tal como te lo dio la tienda");
        }
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            if (documento.getNumberOfPages() > MAX_PAGINAS) {
                throw AppException.badRequest("Ese PDF tiene demasiadas páginas para ser un ticket");
            }
            PDFTextStripper extractor = new PDFTextStripper();
            // Por posición: las columnas de precio quedan en la misma línea que su artículo.
            extractor.setSortByPosition(true);
            return extractor.getText(documento);
        } catch (IOException e) {
            throw AppException.badRequest("No hemos podido abrir ese PDF");
        }
    }
}
