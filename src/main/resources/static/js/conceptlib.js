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

function selectBox(obj){
    var index=$("td[class='select']").index($(obj).parent());
    if($(obj).prop("checked")){        //选中
        item_display["item"][(target_page-1)*10+index].selected=true;
    }
    else{
        item_display["item"][(target_page-1)*10+index].selected=false;
    }
}

function getPage(page){
    var count=0;
    for(var i=(page-1)*10; i<page*10 && i<item_display["item"].length && i<item_num;i++){
        $("#result_table").append($('<tr>' +
            '                           <td class="order">' + (i + 1) + '</td>' +
            '                           <td class="entity">' + item_display["item"][i]["entity"] + '</td>' +
            '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>'+
            '                       </tr>'));
        if(item_display.item[i].selected==true){
            $($(".select_item")[i%10]).prop("checked",true);
            count++;
        }
    }
    if(count==10 || (target_page-1)*10+count==item_display["item"].length)
        $("input[name='select_page']").prop("checked",true);
    else
        $("input[name='select_page']").prop("checked",false);
}

function clear_lib(){
    if(window.confirm('确定要清空本概念?')){
        $.get("clear_concept_lib?field="+GetQueryString("field")+"&concept="+$("#select_concept").find("option:selected").text(),function (data) {
            if(data.statu==0)
                alert('清空失败!');
            else{
                alert('清空成功!');
                if(Object.keys(items).length==2)
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

function clear_selected(){
    var json_del={};
    var k;
    for(k=0;k<Object.keys(items).length;k++){
        var key=Object.keys(items)[k];
        if(key!='statu'){
            var a=new Array();
            for(j in items[key]) {
                if(items[key][j]["selected"])
                    a.push(items[key][j]);
            }
            json_del[key]=a;
        }
    }

    if(window.confirm('确定要删除所选词?')){
        $.ajax({
            type:"POST",
            url:"clear_selected_entity?field="+GetQueryString("field"),
            contentType: "application/json;charset=utf-8",
            data:JSON.stringify(json_del),
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

            item_display.item=items[Object.keys(items)[0]];
            item_display.item.sort(function (a, b) {
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
            item_num = item_display.item.length;

            var i;
            for (i = 0; i < 10 && i < item_num; i++) {
                if (item_display.item[i]["isnew"]) {          //高亮显示
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order" style="color: #1e87f0">' + (i + 1) + '</td>' +
                        '                           <td class="entity" style="color: #1e87f0">' + item_display.item[i]["entity"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>'+
                        '                       </tr>'));
                } else {
                    $("#result_table").append($('<tr>' +
                        '                           <td class="order">' + (i + 1) + '</td>' +
                        '                           <td class="entity">' + item_display.item[i]["entity"] + '</td>' +
                        '                           <td class="select"><input onclick="selectBox(this)" class="select_item" type="checkbox"/></td>'+
                        '                       </tr>'));
                }
            }
            regulatePage(1);
        }
        $("input[name='select_page']").prop("checked",false);

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
        // if($("#select_concept").val()==1){
        //     var allitems=new Array();
        //     var j=0;
        //     for(;j<Object.keys(items).length;j++){
        //         if(Object.keys(items)[j]!='statu') {
        //             var k;
        //             for (k in items[Object.keys(items)[j]]) {
        //                 var entry={};
        //                 entry=items[Object.keys(items)[j]][k];
        //                 entry.concept=Object.keys(items)[j];
        //                 allitems.push(entry);
        //             }
        //         }
        //     }
        //     item_display.item=allitems;
        // }

        item_display.item=items[$("#select_concept").find("option:selected").text()];
        item_display.item.sort(function (a, b) {
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
        item_num=item_display.item.length;
        clearPage();
        getPage(1);
        regulatePage(1);
    });

    $("input[name='select_page']").change(function () {     //本页全选
        if($(this).prop("checked")==true){            //选中
            var i;
            for(i=0;i< 10 && (target_page-1)*10+i<item_display["item"].length;i++){
                item_display.item[(target_page-1)*10+i]["selected"]=true;
                $($("input[class='select_item']")[i]).prop("checked",true);
            }
        }
        else{
            var i;
            for(i=0;i< 10 && (target_page-1)*10+i<item_display["item"].length;i++){
                item_display.item[(target_page-1)*10+i]["selected"]=false;
                $($("input[class='select_item']")[i]).prop("checked",false);
            }
        }

    });
});
