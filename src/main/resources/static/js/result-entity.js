var items={};
var target_page;        //当前在哪页
var page_num;           //可以显示多少条
var select_table=false;
var concept;

function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return decodeURI(r[2]);
    return null;
}



function add_items() {
    if(window.confirm('确定添加所选词？请等待页面自动跳转')){
        $.ajax({
            type: "POST",
            url:"result-entity.html?field=" + GetQueryString("field")+"&name="+GetQueryString("name"),
            contentType: "application/json;charset=utf-8",
            success:function(data){
                $(location).attr("href","conceptlib.html?field="+GetQueryString("field"));
            },
            error:function (message) {
                console.log(message);
            }
        });
    }
}

function getPage(page){
    if(page_num==0)
        return;
    $.ajax({
        type: "POST",
        url: "getPage?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&concept="+concept+"&page="+page,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items[concept]),
        success: function (data) {
            items[concept]["item"]=data["item"];
            for(var i=0; i<10 && i<data["item"].length;i++){
                $("#result_table").append($('<tr>' +
                    '                           <td class="order">' + ((page-1)*10+i+1) + '</td>' +
                    '                           <td class="entity">' + items[concept]["item"][i]["entity"] + '</td>' +
                    '                           <td class="point">' + items[concept]["item"][i]["point"] + '</td>' +
                    '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
                if (items[concept]["item"][i]["isnew"]) {          //高亮显示
                    $($("#result_table tr")[i+1]).css('color','#1e87f0');

                }
                else{
                    $($("#result_table tr")[i+1]).css('color','#212529');
                }
                if(data["item"][i]["selected"]==1 || select_table){
                    $($(".select_item")[i]).prop("checked",true);
                }
            }
        },
        error: function (message) {
            console.log(message);
        }
    });
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
        items[concept]["item"][index].selected=1;
    }
    else{
        items[concept]["item"][index].selected=0;
    }
    items[concept]["item"][index].entity=items[concept]["item"][index].entity.replace(/\+/g,"%2B");
    items[concept]["item"][index].entity=items[concept]["item"][index].entity.replace(/\&/g,"%26");
    $.post("result-select?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&concept="+concept+"&entity="+items[concept]["item"][index].entity+"&selected="+items[concept]["item"][index].selected,function(data,statu){
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

$(function () {
    target_page=1;     //初始时第一页
    $("input[name='whattoshow']").get(1).checked=true;

    $.get("get_concepts?field=" + GetQueryString("field") , function (data, status) {
        var count=1;
        concept=data.concepts[0];
        for(var i=0;i<data["concepts"].length;i++){
            $("#select_concept").append($('<option value="'+(count++)+'">'+data.concepts[i]+'</option>'))
            var filter={};
            filter["neworall"]="all";
            filter["rankorpoint"]="all";
            filter["num"]=-1;
            filter["select"]="none";
            items[data.concepts[i]]={};
            items[data.concepts[i]]["filter"]=filter;
            items[data.concepts[i]]["item"]=new Array();
        }
        $.ajax({
            type:"post",
            url:"getConceptPageNum?field="+GetQueryString("field") + "&name=" + GetQueryString("name") + "&concept=" + data.concepts[0],
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items[data.concepts[0]].filter),
            success: function(result) {
                page_num=result.page_num;
                getPage(1);
                regulatePage(1);
            }
        });

    });


    //点击切换页面标签
    $("ul[class='uk-tab'] li").each(function () {
        $(this).click(function () {
            if($(this).attr("id")!="pagex" && $(this.firstChild).text()!="") {
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
            getPage(parseInt($("#goto").val()));
            regulatePage(parseInt($("#goto").val()));
        }
    });

    $("#goto").focus(function(){
        this.select();
        if($("#goto").val().length>0)
            $(document).keyup(function(e){
                if(e.keyCode==13){
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


    $("#filter").focus(function () {
        this.select();
    });

    $("#confirm").click(function () {
        concept=$("#select_concept").find("option:selected").text();
        $("input[name='select_page']").prop("checked",false);
        $("input[name='select_filtered']").prop("checked",false);
        $("input[name='select_table']").prop("checked",false);


        if($("#select_cond").val()==1){
            /*改状态*/
            items[concept]["filter"]["rankorpoint"]="point";
            items[concept]["filter"]["num"]=-1;
        }
        else if($("#select_cond").val()==2){      //按排序
            items[concept]["filter"]["rankorpoint"]="rank";
            items[concept]["filter"]["num"]=$("#filter").val();
        }
        else if($("#select_cond").val()==3){   //按分数
            items[concept]["filter"]["rankorpoint"]="point";
            items[concept]["filter"]["num"]=$("#filter").val();
        }

        if($("input[name='whattoshow']").get(0).checked==true){
            items[concept]["filter"]["neworall"]="new";
        }
        else items[concept]["filter"]["neworall"]="all";

        if(!isNumber(items[concept]["filter"]["num"]))
            items[concept]["filter"]["num"]=-1;
        /*统计有多少ye*/
        $.ajax({
            type: "POST",
            url: "getConceptPageNum?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&concept="+concept,
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items[concept]["filter"]),
            success: function(result) {
                page_num=result.page_num;
                target_page=1;
                clearPage();
                getPage(1);
                regulatePage(1);
            }
        });
    });

    $("input[name='select_page']").change(function () {     //本页全选
        select_table=false;
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_filtered']").prop("checked",false);
            var i;
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=true;
                selectBox($(".select_item")[i]);
            }
            items[concept]["filter"]["submit"]="page";
        }
        else{
            var i;
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=false;
                selectBox($(".select_item")[i]);
            }
            items[concept]["filter"]["submit"]="table";
        }

    });

    $("input[name='select_filtered']").change(function () {        //﻿筛选结果全选
        select_table=false;
        var flag;
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_page']").prop("checked",false);

            var i;
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=true;
                items[concept]["item"][i].selected=1;
            }
            items[concept]["filter"]["submit"]="table";
            flag=1;
        }
        else{
            var i;
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=false;
                items[concept]["item"][i].selected=0;
            }
            items[concept]["filter"]["submit"]="page";
            flag=0;
        }
        $.ajax({
            type: "POST",
            url: "select_filtered?field=" + GetQueryString("field")+"&name="+GetQueryString("name")+"&concept="+concept+"&flag="+flag,
            contentType: "application/json;charset=utf-8",
            data: JSON.stringify(items[concept]["filter"]),
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
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=true;
                items[concept]["item"][i].selected=1;
            }
            flag=1;
            select_table=true;
        }
        else{
            var i;
            for(i=0;i<items[concept]["item"].length;i++){
                $(".select_item").get(i).checked=false;
                items[concept]["item"][i].selected=0;
            }
            flag=0;
            select_table=false;
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

});

