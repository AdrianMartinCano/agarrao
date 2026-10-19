package dev.pimon.listillo.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AliasTicketRepository extends JpaRepository<AliasTicket, AliasTicket.Clave> {
}
