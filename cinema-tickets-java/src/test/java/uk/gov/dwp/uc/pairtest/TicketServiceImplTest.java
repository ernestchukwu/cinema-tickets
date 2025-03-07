package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class TicketServiceImplTest {

    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketPaymentService = mock(TicketPaymentService.class);
        seatReservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldPurchaseTicketsSuccessfully() {
        long accountId = 1L;
        TicketTypeRequest validAdultTicketsRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest validChildTicketsRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        ticketService.purchaseTickets(accountId, validAdultTicketsRequest, validChildTicketsRequest);

        verify(seatReservationService).reserveSeat(accountId, 3);
        verify(ticketPaymentService).makePayment(accountId, 65);
    }

    @Test
    void shouldThrowExceptionWhenAccountIdIsInvalid() {
        TicketTypeRequest ticketRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, ticketRequest));

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(-1L, ticketRequest));

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, ticketRequest));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldThrowExceptionWhenRequestHasNegativeTickets() {
        TicketTypeRequest invalidRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, invalidRequest));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldThrowExceptionWhenRequestExceedsMaxTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, request));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldThrowExceptionWhenChildOrInfantTicketsArePresentWithoutAdult() {
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childRequest));

        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, infantRequest));

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, childRequest, infantRequest));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldHandleMixedTicketTypesCorrectly() {
        long accountId = 1L;
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(accountId, adultRequest, childRequest, infantRequest);

        // 4 seats (2 adults + 2 children), infants don't need seats
        verify(seatReservationService).reserveSeat(accountId, 4);

        // Total amount = 2 adults (25 * 2 = 50) + 2 children (15 * 2 = 30)
        verify(ticketPaymentService).makePayment(accountId, 80);
    }

    @Test
    void shouldThrowExceptionWhenTicketTypeIsNull() {
        TicketTypeRequest invalidRequest = new TicketTypeRequest(null, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, invalidRequest));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldThrowExceptionWhenNoTicketsAreProvided() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    void shouldThrowExceptionWhenTicketsArrayIsEmpty() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService
                .purchaseTickets(1L, new TicketTypeRequest[0]));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }
}
