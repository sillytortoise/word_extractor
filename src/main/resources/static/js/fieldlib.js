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

function clear_selected(){
    if(window.confirm('确定要删除所选词?')){
        $.ajax({
            type:"POST",
            url:"clear_selected?field="+GetQueryString("field"),
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items),
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
        items.item[(target_page-1)*10+index].selected=true;
    }
    else{
        items.item[(target_page-1)*10+index].selected=false;
    }
}

function getPage(page) {
    var count=0;
    for (var i = (page - 1) * 10; i < page * 10 && i < items["item"].length && i < item_num; i++) {
        if(items["item"][i]["selected"]) {
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox" checked/></td>' +
                '                       </tr>'));
            count++;
        }
        else{
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                '                       </tr>'));
        }
    }
    if(count==10 || (target_page-1)*10+count>=items["item"].length)
        $("input[name='select_page']").prop("checked",true);
    else
        $("input[name='select_page']").prop("checked",false);
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
        items["item"].sort(function (a, b) {
            var i;
            for(i=0;i<a.entity.length;i++){
                if(i>=b.entity.length) {
                    return 1;
                }
                else if(a.entity[i]==b.entity[i])
                    continue;
                else{
                    return a.entity[i].localeCompare(b.entity[i],'zh-CN');
                }
            }
            if(a.entity.length==b.entity.length){
                return 0;
            }
            else{
                return -1;
            }
        });
        var i;

        for (i = 0; i < 10 && i<item_num; i++) {
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>'+
                '                       </tr>'));
        }
        regulatePage(1);
        $("input[name='select_page']").prop("checked",false);
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

    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            var i;
            for(i=0;i< 10 && (target_page-1)*10+i<items["item"].length;i++){
                items.item[(target_page-1)*10+i]["selected"]=true;
                $($("input[class='select_item']")[i]).prop("checked",true);
            }
        }
        else{
            var i;
            for(i=0;i< 10 && (target_page-1)*10+i<items["item"].length;i++){
                items.item[(target_page-1)*10+i]["selected"]=false;
                $($("input[class='select_item']")[i]).prop("checked",false);
            }
        }

    });

});