package dev.pimon.listillo.ticket;

public enum OrigenTicket {
    /** Ticket digital: el texto viene tal cual del PDF. */
    PDF,
    /** Foto leída en el móvil (Tesseract): puede traer erratas. */
    FOTO
}
