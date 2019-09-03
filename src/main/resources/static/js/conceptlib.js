var items;
var target_page;        //当前在哪页
var item_num;           //可以显示多少条
var item_display={};

function GetQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return decodeURI(r[2]);
    return null;
}

function download() {
    $(location).attr("href","/download/conceptlib?field="+GetQueryString("field")+"&concept="+$("#select_concept").find("option:selected").text());
}

function getPage(page){
    for(var i=(page-1)*10; i<page*10 && i<item_display["item"].length && i<item_num;i++){
        if (item_display["item"][i]["isnew"]) {          //高亮显示
            $("#result_table").append($('<tr>' +
                '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                '                           <td class="entity" style="color: #1e87f0">' + item_display["item"][i]["entity"] + '</td>' +
                '                       </tr>'));
        } else {
            $("#result_table").append($('<tr>' +
                '                           <td class="order">' + (i + 1) + '</td>' +
                '                           <td class="entity">' + item_display["item"][i]["entity"] + '</td>' +
                '                       </tr>'));
        }
        if(item_display.item[i].selected==true){
            $($(".select_item")[i%10]).prop("checked",true);
        }
    }
}

function clear_lib(){
    if(window.confirm('确定要清空领域词库?')){
        $.get("clear_concept_lib?field="+GetQueryString("field"),function (data) {
            if(data.statu==0)
                alert('清空失败!');
            else{
                alert('清空成功!');
                $(location).attr("href","index.html");
            }
        })
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
    $("#pagen a").text(item_num % 10 == 0 ? item_num / 10 : Math.ceil(item_num / 10));
    if(target_page!=1){
        $("ul[class='uk-tab'] li").each(function () {
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
        });
    }
    else{
        $("ul[class='uk-tab'] li").each(function () {
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

        });
    }
}

$(function () {
    target_page=1;     //初始时第一页
    $.post("conceptlib?field=" + GetQueryString("field") + "&name=" + GetQueryString("name"), function (data, status) {
        if(data.statu!=null && data.statu==0){
            $(location).attr("href","login.html");
        }
        else {
            items = data;
            var m=0;
            var count=1;
            for(;m<Object.keys(items).length;m++){
                if(Object.keys(items)[m]!='statu')
                    $("#select_concept").append($('<option value="'+(++count)+'">'+Object.keys(items)[m]+'</option>'));
            }

            var allitems=new Array();
            var j=0;
            for(;j<Object.keys(items).length;j++){
                if(Object.keys(items)[j]!='statu') {
                    var k;
                    for (k in items[Object.keys(items)[j]]) {
                        var entry={};
                        entry=items[Object.keys(items)[j]][k];
                        entry.concept=Object.keys(items)[j];
                        allitems.push(entry);
                    }
                }
            }
            item_display.item=allitems;
            item_num = item_display.item.length;

            var i;
            for (i = 0; i < 10 && i < item_num; i++) {
                if (item_display.item[i]["isnew"]) {          //高亮显示
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                        '                           <td class="entity" style="color: #1e87f0">' + item_display.item[i]["entity"] + '</td>' +
                        '                       </tr>'));
                } else {
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order">' + (i + 1) + '</td>' +
                        '                           <td class="entity">' + item_display.item[i]["entity"] + '</td>' +
                        '                       </tr>'));
                }
            }
            regulatePage(1);
        }
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

    $("#select_concept").change(function(){
        if($("#select_concept").val()==1){
            var allitems=new Array();
            var j=0;
            for(;j<Object.keys(items).length;j++){
                if(Object.keys(items)[j]!='statu') {
                    var k;
                    for (k in items[Object.keys(items)[j]]) {
                        var entry={};
                        entry=items[Object.keys(items)[j]][k];
                        entry.concept=Object.keys(items)[j];
                        allitems.push(entry);
                    }
                }
            }
            item_display.item=allitems;
        }
        else{
            item_display.item=items[$("#select_concept").find("option:selected").text()];
        }
        item_num=item_display.item.length;
        clearPage();
        getPage(1);
        regulatePage(1);
    });
});
