var items;
var target_page;        //当前在哪页
var item_num;           //可以显示多少条

function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return decodeURI(r[2]);
    return null;
}

function download() {
    $(location).attr("href","/download/fieldlib?field="+GetQueryString("field"));
}

function clear_lib(){
    if(window.confirm('确定要清空领域词库?')){
        $.get("clearlib?field="+GetQueryString("field"),function (data) {
            if(data.statu==0)
                alert('清空失败!');
            else{
                alert('清空成功!');
                $(location).attr("href","index.html");
            }
        })
    }
}

function getPage(page) {
    for (var i = (page - 1) * 10; i < page * 10 && i < items["item"].length && i < item_num; i++) {
        $("#result_table").append($('<tr>' +
            '                           <td class="order">' + (i + 1) + '</td>' +
            '                           <td class="entity">' + items["item"][i] + '</td>' +
            '                       </tr>'));
    }
}

function clearPage() {
    $("#result_table tr").each(function () {
        if ($(this).children().first().prop("tagName") == "TD") {
            $(this).remove();
        }
    });
    $("ul[class='uk-tab'] li").each(function () {
        $(this).attr("class", "");
    });
}

function regulatePage(target_page) {
    $("#pagen a").text(item_num %10==0 ? item_num/10 : Math.ceil(item_num/10));
    if (target_page != 1) {
        $("ul[class='uk-tab'] li").each(function () {
            if ($(this).attr("id") != "pagen") {
                if ($("ul[class='uk-tab'] li").index(this) == 0) {
                    $(this.firstChild).text(target_page - 1);
                    $(this).attr("class", "");
                } else if ($("ul[class='uk-tab'] li").index(this) == 1) {
                    $(this.firstChild).text(target_page);
                    $(this).attr("class", "uk-active");
                } else if ($("ul[class='uk-tab'] li").index(this) == 2) {
                    if (target_page + 1 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 1);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 3) {
                    if (target_page + 2 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 2);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 4) {
                    if (target_page + 3 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(target_page + 3);
                    }
                }
            }
        });
    } else {
        $("ul[class='uk-tab'] li").each(function () {
            if ($(this).attr("id") != "pagen") {
                if ($("ul[class='uk-tab'] li").index(this) == 0) {
                    $(this.firstChild).text(1);
                    $(this).attr("class", "uk-active");
                } else if ($("ul[class='uk-tab'] li").index(this) == 1) {
                    if (2 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(2);
                    }
                    $(this).attr("class", "");
                } else if ($("ul[class='uk-tab'] li").index(this) == 2) {
                    if (3 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(3);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 3) {
                    if (4 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(4);
                    }
                } else if ($("ul[class='uk-tab'] li").index(this) == 4) {
                    if (5 > parseInt($("#pagen").text())) {
                        $(this.firstChild).text("");
                    } else {
                        $(this.firstChild).text(5);
                    }
                }
            }
        });
    }
}

$(function () {
    target_page = 1;     //初始时第一页
    $.post("fieldlib?field=" + GetQueryString("field"), function (data, status) {
        items = data;
        item_num = data["item"].length;
        var i;
        for (i = 0; i < 10 && i<item_num; i++) {
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + items["item"][i] + '</td>' +
                '                       </tr>'));
        }
        regulatePage(1);
    });

    //点击切换页面标签
    $("ul[class='uk-tab'] li").each(function () {
        $(this).click(function () {
            if ($(this).attr("id") != "pagex" && $(this.firstChild).text() != "") {
                clearPage();
                target_page = parseInt($(this).text());
                getPage(target_page);
                regulatePage(target_page);
            }
        });
    });

    $("#goto_page").click(function () {
        if ($("#goto").val().length > 0) {
            clearPage();
            getPage(parseInt($("#goto").val()));
            regulatePage(parseInt($("#goto").val()));
        }
    });

    $("#goto").focus(function () {
        this.select();
        if ($("#goto").val().length > 0)
            $(document).keyup(function (e) {
                if (e.keyCode == 13) {
                    clearPage();
                    getPage(parseInt($("#goto").val()));
                    regulatePage(parseInt($("#goto").val()));
                }
            });
    });

});