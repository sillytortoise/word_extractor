var items;
var target_page;

function getPage(page){
    for(var i=(page-1)*10; i<page*10 && i<items["item"].length;i++){
        $("#result_table").append($('<tr>' +
            '                           <td class="order">'+(i+1)+'</td>'+
            '                           <td class="entity">'+items["item"][i]["entity"]+'</td>'+
            '                           <td class="point">'+items["item"][i]["point"]+'</td>'+
            '                           <td class="select"><input type="checkbox"/></td>'+
            '                       </tr>'));
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

$(function () {
    target_page=1;     //初始时第一页
    $.post("result_data",function (data, status) {
        items=data;
        $("#pagen a").text(data["item"].length %10==0 ? data["item"].length/10 : Math.ceil(data["item"].length/10));
        var i;
        for(i=0; i<10;i++){
            $("#result_table").append($('<tr>' +
                '                           <td class="order">'+(i+1)+'</td>'+
                '                           <td class="entity">'+data["item"][i]["entity"]+'</td>'+
                '                           <td class="point">'+data["item"][i]["point"]+'</td>'+
                '                           <td class="select"><input type="checkbox"/></td>'+
                '                       </tr>'));
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

    $("select").change(function () {
        if($("select").val()==2){
            alert("haha");
            items["item"].sort(function (a, b) {
               return items["item"][b]["entity"]-items["item"][a]["entity"];
            });
        }
        clearPage();
        getPage(target_page);
        regulatePage(target_page);
    });
});