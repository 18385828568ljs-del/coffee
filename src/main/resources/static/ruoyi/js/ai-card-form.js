(function($){
    function syncProduct(side){
        var $select=$('#'+side+'ProductSelect'),value=$select.val()||'',parts=value.split(':'),$option=$select.find('option:selected');
        $('#'+side+'ProductType').val(parts.length===2?parts[0]:'');
        $('#'+side+'ProductId').val(parts.length===2?parts[1]:'');
        if(parts.length===2){$('#'+side+'ProductName').val($option.data('name')||'');$('#'+side+'ProductImage').val($option.data('image')||'');}
        updatePreview();
    }
    function text(id,fallback){var v=$(id).val();return v&&String(v).trim()?String(v).trim():fallback;}
    function updatePreview(){
        $('#previewEnglish').text(text('#englishTitle','COFFEE CARD').toUpperCase());$('#previewTitle').text(text('#title','今日咖啡'));
        $('#previewLeftName').text(text('#leftProductName','咖啡特调'));$('#previewLeftDesc').text(text('#leftDescription','今日风味推荐'));
        $('#previewRightName').text(text('#rightProductName','咖啡搭子'));$('#previewRightDesc').text(text('#rightDescription','今日风味推荐'));
        $('#previewBrand').text(text('#brandName','COFFEE STREET'));
        $('#cardPreview').removeClass('palette-candy palette-mint palette-mocha').addClass('palette-'+($('#paletteCode').val()||'candy'));
        var logo=$('#logoUrl').val();if(logo){$('#previewLogo').attr('src',logo).show();}else $('#previewLogo').hide();
    }
    window.AiCardForm={init:function(){
        $('#leftProductSelect').on('change',function(){syncProduct('left');});$('#rightProductSelect').on('change',function(){syncProduct('right');});
        $('#card-form').on('input change','input,textarea,select',updatePreview);updatePreview();
    }};
})(jQuery);
