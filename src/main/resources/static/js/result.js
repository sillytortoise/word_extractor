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

function show_new() {
    items.item.sort(function (a, b) {
        if(a.isnew==b.isnew)
            return 0;
        else if(a.isnew==true && b.isnew==false)
            return -1;
        else return 1;
    });
    var i;
    for(i=0;items["item"][i].isnew==true;i++){}
    item_num=i;
    target_page=1;
    clearPage();
    getPage(1);
    regulatePage(1);
    $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
}

function show_all(){
    item_num=items.item.length;
    clearPage();
    getPage(1);
    regulatePage(1);
    $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
}

function add_items() {
    var result_items = new Array();
    for (var i = 0; i < items.item.length; i++) {
        if (items.item[i].isnew == true && items.item[i].selected == true) {
            var item = {};
            item.point = items.item[i].point;
            item.entity = items.item[i].entity;
            result_items.push(item);
        }
    }
    var result = {};
    result.item = result_items;
    $.ajax({
        type: "POST",
        url: "result.html?field=" + GetQueryString("field"),
        contentType: "application/json;charset=utf-8",
        data: JSON.stringify(result),
        success: function () {
            alert("提交成功!");
            $(location).attr("href", "fieldlib.html?field=" + GetQueryString("field"));
        },
        error: function (message) {
            console.log(message);
        }
    });
}

function getPage(page){
    for(var i=(page-1)*10; i<page*10 && i<items["item"].length && i<item_num;i++){
        if (items["item"][i]["isnew"]) {          //高亮显示
            $("#result_table").append($('<tr>' +
                '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                '                           <td class="entity" style="color: #1e87f0">' + items["item"][i]["entity"] + '</td>' +
                '                           <td class="point" style="color: #1e87f0">' + items["item"][i]["point"] + '</td>' +
                '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                '                       </tr>'));
        } else {
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                '                           <td class="point">' + items["item"][i]["point"] + '</td>' +
                '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                '                       </tr>'));
        }
        if(items.item[i].selected==true){
            $($(".select_item")[i%10]).prop("checked",true);
        }
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
                    $(this.firstChild).text(2);
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
    index+=(target_page-1)*10;
    if($(obj).prop("checked")){        //选中
        items.item[index].selected=true;
    }
    else{
        items.item[index].selected=false;
    }
}

$(function () {
    target_page=1;     //初始时第一页
    $.post("result_data?field=" + GetQueryString("field") + "&name=" + GetQueryString("name"), function (data, status) {
        items=data;
        item_num=data["item"].length;
        $("#pagen a").text(data["item"].length %10==0 ? data["item"].length/10 : Math.ceil(data["item"].length/10));
        var i;
        for(i=0; i<10 && i<item_num;i++){
            if (items["item"][i]["isnew"]) {          //高亮显示
                $("#result_table").append($('<tr>' +
                    '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                    '                           <td class="entity" style="color: #1e87f0">' + items["item"][i]["entity"] + '</td>' +
                    '                           <td class="point" style="color: #1e87f0">' + items["item"][i]["point"] + '</td>' +
                    '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
            } else {
                $("#result_table").append($('<tr>' +
                    '                           <td class="order">' + (i + 1) + '</td>' +
                    '                           <td class="entity">' + items["item"][i]["entity"] + '</td>' +
                    '                           <td class="point">' + items["item"][i]["point"] + '</td>' +
                    '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>' +
                    '                       </tr>'));
            }
        }
        regulatePage(1);
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

    $("#confirm").click(function () {
        if($("select").val()==1){
            item_num=items["item"].length;
            target_page=1;
            clearPage();
            $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
            getPage(1);
            regulatePage(1);
        }
        else if($("select").val()==2){   //按字典顺序排序
            items["item"].sort(function (a, b) {
                var i;
                for(i=0;i<a["entity"].length;i++){
                    if(i>=b["entity"].length) {
                        return 1;
                    }
                    else if(a["entity"][i]==b["entity"][i])
                        continue;
                    else{
                        return a["entity"][i].localeCompare(b["entity"][i],'zh-CN');
                    }
                }
                if(a["entity"].length==b["entity"].length){
                    return 0;
                }
                else{
                    return -1;
                }
            });

            var filter_num=$("#filter").val();
            if(filter_num>=1 && filter_num%1==0){   //如果过滤数为整数，显示filter_num条数据
                clearPage();
                item_num=filter_num;
                $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                target_page=1;
                getPage(1);
                regulatePage(1);
            }
            else {
                item_num=items["item"].length;
                $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                clearPage();
                getPage(target_page);
                regulatePage(target_page);
            }

        }
        else if($("select").val()==3){   //按分数由大到小排序
            items["item"].sort(function (a, b) {
               return b["point"]-a["point"];
            });

            if($("#filter").val()!=""&& $("#filter").val()>=0 && $("#filter").val()<=1)
            {     //如果满足过滤条件，显示过滤结果
                var filter_point = $("#filter").val();

                var i;
                var num=0;
                for(i in items["item"]){
                    if(items["item"][i]["point"]<filter_point || num>=100)
                        break;
                    num++;
                }

                item_num = num;
                $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                target_page = 1;
                clearPage();
                getPage(1);
                regulatePage(1);
            }
            else{                       //不满足过滤条件，不过滤直接显示排序结果
                clearPage();
                item_num=items["item"].length;
                $("#pagen a").text(item_num%10==0?item_num/10:Math.ceil(item_num/10));
                getPage(target_page);
                regulatePage(target_page);
            }
        }

    });

    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_table']").prop("checked",false);
            var i;
            for(i=(target_page-1)*10;i<target_page*10;i++){
                items.item[i].selected=true;
            }
            clearPage();
            getPage(target_page);
        }
        else{
            var i;
            for(i=(target_page-1)*10;i<target_page*10;i++){
                items.item[i].selected=false;
            }
            clearPage();
            getPage(target_page);
        }

    });

    $("input[name='select_table']").change(function () {
        if($(this).prop("checked")==true){            //选中
            $("input[name='select_page']").prop("checked",false);
            var i;
            for(i=0;i<item_num;i++){
                items.item[i].selected=true;
            }
        }
        else{
            var i;
            for(i=0;i<item_num;i++){
                items.item[i].selected=false;
            }
        }
        clearPage();
        getPage(target_page);
    });

});