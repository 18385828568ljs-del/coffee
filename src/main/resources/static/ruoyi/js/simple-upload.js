(function(window, $) {
    "use strict";

    function hasAllowedExtension(fileName, allowedExtensions) {
        if (!allowedExtensions || !allowedExtensions.length) {
            return true;
        }
        var ext = (fileName || "").split(".").pop().toLowerCase();
        return $.inArray(ext, allowedExtensions) !== -1;
    }

    function fileNameFromUrl(url) {
        if (!url) {
            return "";
        }
        return url.split("?")[0].split("/").pop();
    }

    function init(options) {
        var settings = $.extend({
            uploadUrl: (window.ctx || "") + "common/upload",
            previewType: "image",
            allowedExtensions: ["jpg", "jpeg", "png", "gif"],
            loadingText: "文件上传中，请稍候...",
            successText: "文件上传成功",
            invalidText: "请选择正确格式的文件",
            failText: "文件上传失败",
            networkFailText: "文件上传失败，请重试"
        }, options || {});

        var $card = $(settings.card);
        var $fileInput = $(settings.fileInput || $card.find("input[type='file']").first());
        var $targetInput = $(settings.targetInput);
        var $empty = $(settings.empty || $card.find(".ruoyi-upload-empty"));
        var $preview = $(settings.preview || $card.find(".ruoyi-upload-preview"));
        var $previewImage = $(settings.previewImage || $card.find("img"));
        var $previewText = $(settings.previewText || $card.find(".ruoyi-upload-file small"));
        var $changeButton = $(settings.changeButton || $card.find("[data-upload-action='change']"));
        var $removeButton = $(settings.removeButton || $card.find("[data-upload-action='remove']"));

        function openPicker() {
            if ($fileInput.length && $fileInput[0]) {
                $fileInput[0].click();
            }
        }

        function setPreview(url) {
            var hasFile = !!url;
            $targetInput.val(url || "");
            $card.toggleClass("has-file", hasFile);
            $preview.toggle(hasFile);
            $empty.toggle(!hasFile);

            if (settings.previewType === "image") {
                $previewImage.attr("src", hasFile ? url : "");
            } else {
                $previewText.text(hasFile ? fileNameFromUrl(url) || url : "");
            }

            if (!hasFile) {
                $fileInput.val("");
            }
        }

        function upload(file) {
            if (!file) {
                return;
            }
            if (!hasAllowedExtension(file.name, settings.allowedExtensions)) {
                $.modal.alertWarning(settings.invalidText);
                $fileInput.val("");
                return;
            }

            var formData = new FormData();
            formData.append("file", file);
            $.modal.loading(settings.loadingText);
            $.ajax({
                url: settings.uploadUrl,
                type: "post",
                data: formData,
                cache: false,
                contentType: false,
                processData: false,
                success: function(result) {
                    $.modal.closeLoading();
                    if (result.code === 0 && result.url) {
                        setPreview(result.url);
                        $.modal.msgSuccess(settings.successText);
                        return;
                    }
                    $.modal.alertError(result.msg || settings.failText);
                },
                error: function() {
                    $.modal.closeLoading();
                    $.modal.alertError(settings.networkFailText);
                }
            });
        }

        $card.on("click", function(event) {
            if ($(event.target).closest("button,input[type='file']").length) {
                return;
            }
            openPicker();
        });

        $card.on("dragenter dragover", function(event) {
            event.preventDefault();
            event.stopPropagation();
            $card.addClass("is-dragover");
        });

        $card.on("dragleave dragend drop", function(event) {
            event.preventDefault();
            event.stopPropagation();
            $card.removeClass("is-dragover");
        });

        $card.on("drop", function(event) {
            var files = event.originalEvent.dataTransfer.files;
            if (files && files.length) {
                upload(files[0]);
            }
        });

        $fileInput.on("change", function() {
            if (this.files && this.files.length) {
                upload(this.files[0]);
            }
        });

        $changeButton.on("click", openPicker);
        $removeButton.on("click", function() {
            setPreview("");
        });

        setPreview(settings.initialUrl || $targetInput.val());

        return {
            open: openPicker,
            setPreview: setPreview,
            upload: upload
        };
    }

    window.RuoYiSimpleUpload = {
        init: init
    };
})(window, jQuery);
