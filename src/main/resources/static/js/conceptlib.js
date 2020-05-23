function download() {
    $(location).attr("href","/download/conceptlib?field="+GetQueryString("field")+"&concept="+$("#select_concept").find("option:selected").text());
}


function clear_concept() {
    if(window.confirm('确定要清空本概念?')){
        $.get("clear_concept_lib?field="+GetQueryString("field")+"&concept="+$("#select_concept").find("option:selected").text(),function (data) {
            if(data.statu==0)
                alert('清空失败!');
            else{
                alert('清空成功!');
                $(location).attr("href", "conceptlib.html?field=" + GetQueryString("field"));
            }
        })
    }
}

function clear_lib(){
    if(window.confirm('确定要清空词库?')){
        $.get("clear_concept_lib_total?field="+GetQueryString("field"),function (data) {
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
            url:"clear_selected_entity?field="+GetQueryString("field"),
            contentType: "application/json;charset=utf-8",
            success:function (data) {
                alert('删除成功!');
                $(location).attr("href", "conceptlib.html?field=" + GetQueryString("field"));
            },
            error:function (data) {
                alert('删除失败！');
            }
        })
    }
}


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



function getPage(page){
    if(page_num==0)
        return;
    $.ajax({
        type: "POST",
        url: "getConceptLibPage?field=" + GetQueryString("field")+"&concept="+concept+"&page="+page,
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(items),
        success: function (data) {
            items["item"]=data["item"];
            for(var i=0; i<10 && i<data["item"].length;i++){
                $("#result_table").append($('<tr>' +
                    '                           <td class="order">' + ((page-1)*10+i+1) + '</td>' +
                    '                           <td class="entity">' + items["item"][i]["entity"] + '</td>'+
                    '                           <td class="select"><input onclick="selectBox(this)" class="form-check-input select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
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
        items["item"][index].selected=1;
    }
    else{
        items["item"][index].selected=0;
    }
    items["item"][index].entity=items["item"][index].entity.replace(/\+/g,"%2B");
    items["item"][index].entity=items["item"][index].entity.replace(/\&/g,"%26");
    $.post("result-select?field=" + GetQueryString("field")+"&concept="+concept+"&entity="+items["item"][index].entity+"&selected="+items["item"][index].selected,function(data,statu){
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

    $.get("get_concepts?field=" + GetQueryString("field") , function (data, status) {
        var count=1;
        concept=data.concepts[0];
        for(var i=0;i<data["concepts"].length;i++){
            $("#select_concept").append($('<option value="'+(count++)+'">'+data.concepts[i]+'</option>'))
            items["item"]=new Array();
        }
        $.ajax({
            type:"post",
            url:"getConceptPageNum?field="+GetQueryString("field") + "&concept=" + concept,
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(items["item"]),
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


   $("#select_concept").change(function () {
       concept=$("#select_concept").find("option:selected").text();
       clearPage();
       $.ajax({
           type:"post",
           url:"getConceptPageNum?field="+GetQueryString("field") + "&concept=" + concept,
           contentType: "application/json;charset=utf-8",
           data:JSON.stringify(items["item"]),
           success: function(result) {
               page_num=result.page_num;
               getPage(1);
               regulatePage(1);
           }
       });
   })

    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=true;
                selectBox($(".select_item")[i]);
            }
        }
        else{
            var i;
            for(i=0;i<items["item"].length;i++){
                $(".select_item").get(i).checked=false;
                selectBox($(".select_item")[i]);
            }
        }
    });


});
