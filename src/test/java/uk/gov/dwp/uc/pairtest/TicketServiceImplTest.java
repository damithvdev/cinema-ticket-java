package uk.gov.dwp.uc.pairtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    TicketService ticketService;
    TicketPaymentService ticketPaymentService;
    SeatReservationService seatReservationService;

    @Before
    public void setUp() {
        ticketPaymentService = mock(TicketPaymentServiceImpl.class);
        seatReservationService = mock(SeatReservationServiceImpl.class);

        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    public void testAllTicketTypesPurchase() {
        TicketTypeRequest adultTicket = createTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childTicket = createTypeRequest(TicketTypeRequest.Type.CHILD, 8);
        TicketTypeRequest infantTicket = createTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(1L, adultTicket, childTicket, infantTicket);
        verify(ticketPaymentService, times(1)).makePayment(1L, 280);
        verify(seatReservationService, times(1)).reserveSeat(1L, 18);

    }

    @Test
    public void testOnlyAdultTicketsPurchase() {
        TicketTypeRequest adultTicket = createTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        ticketService.purchaseTickets(1L, adultTicket);
        verify(ticketPaymentService, times(1)).makePayment(1L, 200);
        verify(seatReservationService, times(1)).reserveSeat(1L, 10);
    }

    @Test
    public void shouldOnlyChargeForAdultAndChildTickets() {
        TicketTypeRequest adultTicket = createTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = createTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest infantTicket = createTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(1L, adultTicket, childTicket, infantTicket);
        verify(ticketPaymentService).makePayment(1L, 70);
    }

    @Test
    public void shouldBookSeatsForAdultAndChildTickets() {
        TicketTypeRequest adultTicket = createTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = createTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest infantTicket = createTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(1L, adultTicket, childTicket, infantTicket);
        verify(seatReservationService).reserveSeat(1L, 5);
    }

    @Test
    public void testMoreThanTwentyTicketsShouldThrowInvalidPurchaseException() {
        TicketTypeRequest adultTickets = createTypeRequest(TicketTypeRequest.Type.ADULT, 21);
        InvalidPurchaseException e = Assert.assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultTickets));
        assertThat(e.getMessage(), containsString("Can not buy more than 20 tickets at a time"));

    }

    @Test(expected = InvalidPurchaseException.class)
    public void testInvalidPurchaseException() {
        TicketTypeRequest adultTicket = createTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        ticketService.purchaseTickets(1L, adultTicket);
    }

    @Test
    public void testOnlyChildTicketsPurchaseShouldThrowInvalidPurchaseException() {
        TicketTypeRequest childTickets = createTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        InvalidPurchaseException e = Assert.assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, childTickets));
        assertThat(e.getMessage(), containsString("Can not buy tickets without Adult(s)"));

    }

    @Test
    public void testOnlyInfantTicketsPurchaseShouldThrowInvalidPurchaseException() {
        TicketTypeRequest infantTickets = createTypeRequest(TicketTypeRequest.Type.INFANT, 10);
        InvalidPurchaseException e = Assert.assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, infantTickets));
        assertThat(e.getMessage(), containsString("Can not buy tickets without Adult(s)"));

    }

    @Test
    public void testAccountIDLessThanOne() {
        TicketTypeRequest adultTickets = createTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        InvalidPurchaseException e = Assert.assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, adultTickets));
        assertThat(e.getMessage(), containsString("Invalid account id"));
    }

    @Test
    public void testTicketTypeRequestWhenNull() {
        InvalidPurchaseException e = Assert.assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));
        assertThat(e.getMessage(), containsString("Invalid TicketTypeRequest"));
    }

    private TicketTypeRequest createTypeRequest(TicketTypeRequest.Type type, int numberOfTickets) {
        return new TicketTypeRequest(type, numberOfTickets);
    }
}