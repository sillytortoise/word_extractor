var items={};
var target_page;        //当前在哪页
var page_num;

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

function clear_selected(){
    if(window.confirm('确定要删除所选词?')){
        $.ajax({
            type:"POST",
            url:"clear_selected?field="+GetQueryString("field"),
            contentType: "application/json;charset=utf-8",
            success:function (data) {
                alert('删除成功!');
                $(location).attr("href", "fieldlib.html?field=" + GetQueryString("field"));
            },
            error:function (data) {
                alert('删除失败！');
            }
        })
    }
}

function selectBox(obj){
    var index=$("td[class='select']").index($(obj).parent());
    if($(obj).prop("checked")){        //选中
        items.item[index].selected=1;
    }
    else {
        items.item[index].selected = 0;
    }
    items.item[index].entity=items.item[index].entity.replace(/\+/g,"%2B");
    items.item[index].entity=items.item[index].entity.replace(/\&/g,"%26");
    $.post("result-select?field=" + GetQueryString("field")+"&entity="+items.item[index].entity+"&selected="+items.item[index].selected,function(data,statu){
    });
}

function getPage(page){
    if(page_num==0)
        return;
    $.ajax({
        type: "POST",
        url: "getFieldLibPage?field=" + GetQueryString("field")+"&page="+page,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items),
        success: function (data) {
            var count=0;
            items["item"]=data["item"];
            for(var i=0; i<10 && i<data["item"].length;i++){
                $("#result_table").append($('<tr>' +
                    '                           <td class="order">' + ((page-1)*10+i+1) + '</td>' +
                    '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                    '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
                if(data["item"][i]["selected"]==1){
                    $($(".select_item")[i]).prop("checked",true);
                    count++;
                }
            }
            if(count==data["item"].length && $("input[name='select_table']").prop("checked")==false){
                $("input[name='select_page']").prop("checked",true);
            }
            else
                $("input[name='select_page']").prop("checked",false);
        },
        error: function (message) {
            console.log(message);
        }
    });

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
    $("#pagen a").text(page_num);
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
    $.ajax({
        type: "POST",
        url: "getPageNum?field=" + GetQueryString("field")+"&name="+GetQueryString("name"),
        contentType: "application/json;charset=utf-8",
        data:JSON.stringify(items),
        success:function(data,statu){
            page_num=data.page_num;
            target_page=1;
            clearPage();
            items["item"]=new Array();
            getPage(target_page);
            $("#pagen a").text(page_num);
            regulatePage(1);
        }
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

    $("#firstPage").click(function () {
        if(page_num>0){
            target_page=1;
            clearPage();
            getPage(1);
            regulatePage(1);
        }
    });

    $("input[name='select_page']").change(function () {     //本页全选
        if ($(this).prop("checked") == true) {            //选中
            $("input[name='select_table']").prop("checked", false);
            var i;
            for (i = 0; i < items["item"].length; i++) {
                $(".select_item").get(i).checked = true;
            }
            for (var j = 0; j < items["item"].length; j++) {
                selectBox($(".select_item")[j]);
            }
        } else {
            var i;
            for (i = 0; i < items["item"].length; i++) {
                $(".select_item").get(i).checked = false;
            }
            for (var j = 0; j < items["item"].length; j++) {
                selectBox($(".select_item")[j]);
            }
        }
    });

});