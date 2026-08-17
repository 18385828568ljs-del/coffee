package com.ruoyi.project.coffee.decorator.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.domain.DecoratorAsset;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAssetMapper;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;

class DecoratorAssetServiceTest
{
    @Mock
    private DecoratorAssetMapper assetMapper;

    @Mock
    private TenantContextService contextService;

    @Mock
    private FileStorageService fileStorageService;

    private DecoratorAssetService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new DecoratorAssetService();
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(service, "assetMapper", assetMapper);
        ReflectionTestUtils.setField(service, "contextService", contextService);
        ReflectionTestUtils.setField(service, "fileStorageService", fileStorageService);
    }

    @Test
    void uploadStoresImageMetadataAndChecksum() throws Exception
    {
        byte[] content = png(3, 2);
        MockMultipartFile file = new MockMultipartFile("file", "brand.png", "image/png", content);
        when(fileStorageService.upload(file)).thenReturn(
                new StoredFileInfo("/profile/upload/brand.png", "brand.png", "brand.png", "brand.png"));
        when(assetMapper.insertAsset(any(DecoratorAsset.class))).thenAnswer(invocation -> {
            invocation.<DecoratorAsset>getArgument(0).setId(91L);
            return 1;
        });
        when(assetMapper.selectAsset(7L, 91L)).thenAnswer(invocation -> insertedAsset());

        DecoratorAsset result = service.upload(context(), "logo", " Brand mark ", null, file);

        assertEquals(Long.valueOf(91L), result.getId());
        org.mockito.ArgumentCaptor<DecoratorAsset> captor = org.mockito.ArgumentCaptor.forClass(DecoratorAsset.class);
        verify(assetMapper).insertAsset(captor.capture());
        DecoratorAsset inserted = captor.getValue();
        assertEquals("LOGO", inserted.getAssetType());
        assertEquals("Brand mark", inserted.getName());
        assertEquals(Integer.valueOf(3), inserted.getWidth());
        assertEquals(Integer.valueOf(2), inserted.getHeight());
        assertEquals(Long.valueOf(content.length), inserted.getByteSize());
        assertEquals(hex(MessageDigest.getInstance("SHA-256").digest(content)), inserted.getChecksumSha256());
        assertEquals("APPROVED", inserted.getAuditStatus());
    }

    @Test
    void uploadRejectsUnsupportedOrOversizedFiles() throws Exception
    {
        MockMultipartFile text = new MockMultipartFile("file", "note.txt", "text/plain", "not-image".getBytes());
        assertThrows(ServiceException.class, () -> service.upload(context(), "LOGO", null, null, text));

        MultipartFile oversized = org.mockito.Mockito.mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(10L * 1024L * 1024L + 1L);
        assertThrows(ServiceException.class, () -> service.upload(context(), "LOGO", null, null, oversized));
        verify(fileStorageService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void archiveUsesMerchantBoundary()
    {
        when(assetMapper.selectAsset(7L, 81L)).thenReturn(asset(81L, "LOGO"));
        when(assetMapper.archiveAsset(7L, 81L)).thenReturn(1);

        service.archive(context(), 81L);

        verify(assetMapper).selectAsset(7L, 81L);
        verify(assetMapper).archiveAsset(7L, 81L);
        verify(assetMapper, never()).selectAsset(8L, 81L);
    }

    @Test
    void archiveRejectsPublishedAsset()
    {
        when(assetMapper.selectAsset(7L, 81L)).thenReturn(asset(81L, "LOGO"));
        when(assetMapper.countPublishedReferences(7L, 81L)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.archive(context(), 81L));

        verify(assetMapper, never()).archiveAsset(7L, 81L);
    }

    @Test
    void validateReferencesRejectsWrongPurposeOrOtherTenant() throws Exception
    {
        JsonNode logoConfig = objectMapper.readTree("{\"brand\":{\"logoAssetId\":\"51\"},\"components\":{}}");
        when(assetMapper.selectUsableAssets(7L, Collections.singletonList(51L)))
                .thenReturn(Collections.singletonList(asset(51L, "HEADER")));
        assertThrows(ServiceException.class, () -> service.validateReferences(7L, logoConfig));

        JsonNode headerConfig = objectMapper.readTree("{\"brand\":{\"headerAssetId\":\"52\"},\"components\":{}}");
        when(assetMapper.selectUsableAssets(7L, Collections.singletonList(52L)))
                .thenReturn(Collections.singletonList(asset(52L, "LOGO")));
        assertThrows(ServiceException.class, () -> service.validateReferences(7L, headerConfig));

        JsonNode backgroundConfig = objectMapper.readTree("{\"brand\":{},\"components\":{"
                + "\"shopHeader\":{\"background\":{\"assetId\":\"53\"}}}}");
        when(assetMapper.selectUsableAssets(7L, Collections.singletonList(53L)))
                .thenReturn(Collections.singletonList(asset(53L, "HEADER")));
        assertThrows(ServiceException.class, () -> service.validateReferences(7L, backgroundConfig));

        JsonNode otherTenantConfig = objectMapper.readTree("{\"brand\":{\"headerAssetId\":\"61\"},\"components\":{}}");
        when(assetMapper.selectUsableAssets(7L, Collections.singletonList(61L)))
                .thenReturn(Collections.<DecoratorAsset>emptyList());
        assertThrows(ServiceException.class, () -> service.validateReferences(7L, otherTenantConfig));
    }

    @Test
    void replaceDraftReferencesCreatesUsageSnapshot() throws Exception
    {
        JsonNode config = objectMapper.readTree("{\"brand\":{\"logoAssetId\":\"51\"},\"components\":{"
                + "\"productCard\":{\"background\":{\"assetId\":\"71\"}}}}");

        service.replaceDraftReferences(7L, 11L, 21L, config);

        verify(assetMapper).deleteDraftReferences(7L, 11L, 21L);
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 51L, "brand.logo");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 71L,
                "components.productCard.background");
    }

    @Test
    void replaceDraftReferencesCollectsCanonicalSkinAssets() throws Exception
    {
        JsonNode config = objectMapper.readTree("{\"assets\":{"
                + "\"homeBanner\":[101,\"111\",\"/static/banner.png\"],\"actionCard\":\"102\",\"sectionBanner\":null,"
                + "\"aboutImage\":109,\"productCard\":\"/static/product.png\",\"specPanel\":103,"
                + "\"emptyCart\":104,\"cartPanel\":105,\"checkoutBar\":106,"
                + "\"memberCard\":107,\"tabBar\":108},"
                + "\"productImages\":{\"1\":110,\"2\":\"/static/product-2.png\"}}" );

        service.replaceDraftReferences(7L, 11L, 21L, config);

        verify(assetMapper).insertReference(7L, 11L, 21L, null, 101L, "assets.homeBanner.0");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 111L, "assets.homeBanner.1");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 102L, "assets.actionCard");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 109L, "assets.aboutImage");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 103L, "assets.specPanel");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 104L, "assets.emptyCart");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 105L, "assets.cartPanel");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 106L, "assets.checkoutBar");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 107L, "assets.memberCard");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 108L, "assets.tabBar");
        verify(assetMapper).insertReference(7L, 11L, 21L, null, 110L, "productImages.1");
    }

    private DecoratorAsset insertedAsset()
    {
        return asset(91L, "LOGO");
    }

    private static DecoratorAsset asset(Long id, String type)
    {
        DecoratorAsset asset = new DecoratorAsset();
        asset.setId(id);
        asset.setMerchantId(7L);
        asset.setAssetType(type);
        asset.setStatus("ACTIVE");
        asset.setAuditStatus("APPROVED");
        return asset;
    }

    private static TenantContext context()
    {
        return new TenantContext(9L, 8L, 7L, "OWNER", "ALL", Collections.<Long>emptySet());
    }

    private static byte[] png(int width, int height) throws Exception
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String hex(byte[] bytes)
    {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) value.append(String.format("%02x", item & 0xff));
        return value.toString();
    }
}
