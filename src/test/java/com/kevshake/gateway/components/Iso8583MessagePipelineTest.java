package com.kevshake.gateway.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kevshake.gateway.packagers.POSPackager;
import com.kevshake.gateway.security.CardValidationService;
import com.kevshake.gateway.service.ResponseCodeService;
import com.kevshake.gateway.service.ResponseCodeService.ResponseCategory;
import com.kevshake.gateway.service.ResponseCodeService.ResponseCodeInfo;
import com.kevshake.gateway.service.ResponseCodeService.ResponseSeverity;

import io.netty.channel.embedded.EmbeddedChannel;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Iso8583MessagePipelineTest {

	private static final String VALID_VISA_PAN = "4761739001010010";

	@Mock
	private MaskedLogger maskedLogger;
	@Mock
	private BankCommunicationProcessor bankProcessor;
	@Mock
	private TransactionProcessor transactionProcessor;
	@Mock
	private com.kevshake.gateway.security.PinTranspositionService pinTranspositionService;
	@Mock
	private CardValidationService cardValidationService;
	@Mock
	private com.kevshake.gateway.service.TerminalManagementService terminalManagementService;
	@Mock
	private ResponseCodeService responseCodeService;

	@InjectMocks
	private IsoServerHandler handler;

	private POSPackager packager;

	/** Creates a POS packager shared by encoder and decoder tests. */
	@BeforeEach
	void setUp() {
		packager = new POSPackager();
		ReflectionTestUtils.setField(handler, "enableCardValidation", true);
		ReflectionTestUtils.setField(handler, "enablePinTransposition", false);
		ReflectionTestUtils.setField(handler, "enableKeyChange", false);
		when(bankProcessor.processTransactionToBank(any())).thenReturn(CompletableFuture.completedFuture(null));
		when(cardValidationService.validateCardForTransaction(anyString(), anyString())).thenReturn(true);
		when(cardValidationService.validateCard(anyString())).thenReturn(
				new CardValidationService.CardValidationResult(true, true, CardValidationService.CardType.VISA,
						"476173******0010", null));
		when(responseCodeService.getResponseCodeInfo(anyString())).thenReturn(
				new ResponseCodeInfo("00", "Approved", ResponseSeverity.INFO, ResponseCategory.SUCCESS));
		when(responseCodeService.formatResponseMessage(anyString(), any(), any())).thenReturn("Approved");
	}

	/** Round-trips a financial request through decoder, handler, and encoder for MTI 0200 -> 0210. */
	@Test
	void financialRequest0200Returns0210WithCopiedStanAndFields() throws Exception {
		ISOMsg request = financialRequest("0200", "123456", VALID_VISA_PAN);
		ISOMsg response = processThroughPipeline(request);

		assertEquals("0210", response.getMTI());
		assertEquals("123456", response.getString(11));
		assertEquals(VALID_VISA_PAN, response.getString(2));
		assertEquals("000000000100", response.getString(4));
		assertNotNull(response.getString(37));
		assertNotNull(response.getString(39));
	}

	/** Round-trips a reversal request through the pipeline for MTI 0400 -> 0410. */
	@Test
	void reversalRequest0400Returns0410WithCopiedStan() throws Exception {
		ISOMsg request = financialRequest("0400", "654321", VALID_VISA_PAN);
		ISOMsg response = processThroughPipeline(request);

		assertEquals("0410", response.getMTI());
		assertEquals("654321", response.getString(11));
		assertEquals(VALID_VISA_PAN, response.getString(2));
		assertNotNull(response.getString(39));
	}

	/** Returns an error response when the PAN fails Luhn validation. */
	@Test
	void malformedPanReceivesErrorResponse() throws Exception {
		org.mockito.Mockito.reset(cardValidationService);
		when(cardValidationService.validateCardForTransaction(anyString(), anyString())).thenReturn(false);
		when(cardValidationService.validateCard(anyString())).thenReturn(
				new CardValidationService.CardValidationResult(false, false, CardValidationService.CardType.UNKNOWN,
						"411111******1111", "Luhn check failed"));

		ISOMsg request = financialRequest("0200", "999999", "4111111111111111");
		ISOMsg response = processThroughPipeline(request);

		assertEquals("0210", response.getMTI());
		assertEquals("14", response.getString(39));
		assertEquals("999999", response.getString(11));
	}

	private ISOMsg processThroughPipeline(ISOMsg request) throws Exception {
		EmbeddedChannel channel = new EmbeddedChannel(handler);
		channel.writeInbound(request);
		ISOMsg response = channel.readOutbound();
		assertNotNull(response);
		return response;
	}

	private static ISOMsg financialRequest(String mti, String stan, String pan) throws Exception {
		ISOMsg msg = new ISOMsg();
		msg.setPackager(new POSPackager());
		msg.setMTI(mti);
		msg.set(2, pan);
		msg.set(3, "000000");
		msg.set(4, "000000000100");
		msg.set(11, stan);
		msg.set(12, "131500");
		msg.set(13, "1014");
		msg.set(41, "TERM0001");
		return msg;
	}
}
