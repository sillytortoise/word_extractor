var items={};
var target_page;        //当前在哪页
var filter={};
var page_num;

function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return decodeURI(r[2]);
    return null;
}


function add_items() {
    if(window.confirm('确定添加所选词？请等待页面自动跳转')) {
        $.post("result.html?field=" + GetQueryString("field") + "&name=" + GetQueryString("name"), function (data) {
            $(window).attr('location', 'fieldlib.html?field=' + GetQueryString("field"));
        });
    }
}

function getPage(page){
    if(page_num==0)
        return;
    $.ajax({
        type: "POST",
        url: "getPage?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&page="+page,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items),
        success: function (data) {
            items["item"]=data["item"];
            for(var i=0; i<10 && i<data["item"].length;i++){
                $("#result_table").append($('<tr>' +
                    '                           <td class="order">' + ((page-1)*10+i+1) + '</td>' +
                    '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                    '                           <td class="point">' + items["item"][i]["point"] + '</td>' +
                    '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
                if (items["item"][i]["isnew"]) {          //高亮显示
                    $($("#result_table tr")[i+1]).css('color','#1e87f0');

                }
                else{
                    $($("#result_table tr")[i+1]).css('color','#212529');

                }
                if(data["item"][i]["selected"]==1){
                    $($(".select_item")[i]).prop("checked",true);
                }
            }
        },
        error: function (message) {
            console.log(message);
        }
    });

}

function isNumber(val){
    var regPos = /^\d+(\.\d+)?$/; //非负浮点数
    var regNeg = /^(-(([0-9]+\.[0-9]*[1-9][0-9]*)|([0-9]*[1-9][0-9]*\.[0-9]+)|([0-9]*[1-9][0-9]*)))$/; //负浮点数
    if(regPos.test(val) || regNeg.test(val)){
        return true;
    }else{
        return false;
    }
}

function clearPage(){
    $("#result_table tr").each(function () {
        if($(this).children().first().prop("tagName")=="TD"){
            $(this).remove();
        }
    });
    $("ul[class='uk-tab'] li").each(function () {
        $(this).attr("class", "");
    });
}

function regulatePage(target_page){
    $("#pagen a").text(page_num);
    if(target_page!=1){
        $("ul[class='uk-tab'] li").each(function () {
            if($(this).attr("id")!="pagen") {
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
    }
    else{
        $("ul[class='uk-tab'] li").each(function () {
            if($(this).attr("id")!="pagen") {
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

function selectBox(obj){
    var index=$("td[class='select']").index($(obj).parent());
    if($(obj).prop("checked")){        //选中
        items.item[index].selected=1;
    }
    else{
        items.item[index].selected=0;
    }
    items.item[index].entity=items.item[index].entity.replace(/\+/g,"%2B");
    items.item[index].entity=items.item[index].entity.replace(/\&/g,"%26");
    $.post("result-select?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&entity="+items.item[index].entity+"&selected="+items.item[index].selected,function(data,statu){
    });
}

$(function () {
    target_page=1;     //初始时第一页
    filter["neworall"]="all";
    filter["rankorpoint"]="all";
    filter["num"]=-1;
    filter["select"]="none";        //or table
    items["filter"]=filter;
    items["filter"]["submit"]="page";
    $("input[name='whattoshow']").get(1).checked=true;

    $.ajax({
        type: "POST",
        url: "getPageNum?field=" + GetQueryString("field")+"&name="+GetQueryString("name"),
        contentType: "application/json;charset=utf-8",
        data:JSON.stringify(items.filter),
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
            if($(this).attr("id")!="pagex" && $(this.firstChild).text()!="" && $(this.firstChild).text()!="NaN") {
                clearPage();
                target_page=parseInt($(this).text());
                getPage(target_page);
                regulatePage(target_page);
            }
        });
    });

    $("#goto_page").click(function(){
        if($("#goto").val().length>0) {
            clearPage();
            target_page=$("#goto").val();
            getPage(target_page);
            regulatePage(parseInt($("#goto").val()));
        }
    });

    $("#goto").focus(function(){
        this.select();
        if($("#goto").val().length>0)
        $(document).keyup(function(e){
            if(e.keyCode==13){
                clearPage();
                target_page=$("#goto").val();
                getPage(target_page);
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


    $("#confirm").click(function () {
        $("input[name='select_page']").prop("checked",false);
        $("input[name='select_filtered']").prop("checked",false);
        $("input[name='select_table']").prop("checked",false);
        if($("select").val()==1){
            /*改状态*/
            items["filter"]["rankorpoint"]="point";
            items["filter"]["num"]=-1;
        }
        else if($("select").val()==2){      //按排序
            items["filter"]["rankorpoint"]="rank";
            items["filter"]["num"]=$("#filter").val();
        }
        else if($("select").val()==3){   //按分数
            items["filter"]["rankorpoint"]="point";
            items["filter"]["num"]=$("#filter").val();
        }
        if($("input[name='whattoshow']").get(0).checked==true){
            items["filter"]["neworall"]="new";
        }
        else items["filter"]["neworall"]="all";
        if(!isNumber(items["filter"]["num"]))
            items["filter"]["num"]=-1;
        /*统计有多少ye*/
        $.ajax({
            type: "POST",
            url: "getPageNum?field=" + GetQueryString("field")+"&name="+GetQueryString("name"),
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items.filter),
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

    });


    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_filtered']").prop("checked",false);
            $("input[name='select_table']").prop("checked",false);

            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=true;
                selectBox($(".select_item")[i]);
            }
            items["filter"]["submit"]="page";
        }
        else{
            var i;
            for (i = 0; i < items["item"].length; i++) {
                $(".select_item").get(i).checked = false;
                selectBox($(".select_item")[i]);

            }
            items["filter"]["submit"]="none";
        }

    });


    $("input[name='select_filtered']").change(function () {        //﻿筛选结果全选
        var flag;
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_page']").prop("checked",false);
            $("input[name='select_table']").prop("checked",false);

            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=true;
                items["item"][i].selected=1;
            }
            items["filter"]["submit"]="table";
            flag=1;
        }
        else{
            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=false;
                items["item"][i].selected=0;
            }
            items["filter"]["submit"]="page";
            flag=0;
        }
        $.ajax({
            type: "POST",
            url: "select_filtered?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&flag="+flag,
            contentType: "application/json;charset=utf-8",
            data: JSON.stringify(items["filter"]),
            success: function (data) {
            },
            error: function (message) {
                console.log(message);
            }
        });

    });

    $("input[name='select_table']").change(function () {
        var flag;
        if($(this).prop("checked")==true) {            //选中
            $("input[name='select_page']").prop("checked", false);
            $("input[name='select_filtered']").prop("checked", false);
            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=true;
                items["item"][i].selected=1;
            }
            flag=1;
        }
        else{
            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=false;
                items["item"][i].selected=0;
            }
            flag=0;
        }
        $.ajax({
            type: "POST",
            url: "select_table?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&flag="+flag,
            contentType: "application/json;charset=utf-8",
            success: function (data) {
            },
            error: function (message) {
                console.log(message);
            }
        });
    });


    $("#filter").focus(function () {
        this.select();
    });

});
