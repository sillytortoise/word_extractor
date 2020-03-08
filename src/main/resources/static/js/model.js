var field_seed;
var concept;
var record = new Array();     //记录每一个概念词下种子词的个数

function GetQueryString(name) {
    var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if(r!=null)
        return  decodeURI(r[2]);
    return null;
}
function del(obj){
    var thisobj=$(obj);         //js对象转jquery对象
    var id= thisobj.attr("id");
    var num=id.substr(11);
    $("#field_seed_li"+num).remove();
    $("li[id^='field_seed_li']").each(function () {
        $(this).attr("id", "field_seed_li" + ($("li[id^='field_seed_li']").index(this) - 0 + 1));
        $(this.firstChild).attr("name", "field_seed_input" + ($("li[id^='field_seed_li']").index(this) - 0 + 1));
        $(this.lastChild).attr("id", "delete_seed" + ($("li[id^='field_seed_li']").index(this) - 0 + 1));
    });
}
function del_concept(obj){
    var thisobj=$(obj);
    var id=thisobj.attr("id");
    var num=id.substr(14);
    $("#add_concept"+num).remove();
    $("div[id^='add_concept']").each(function () {
        var c_index = $("div[id^='add_concept']").index(this) + 1;
        $(this).attr("id", "add_concept" + c_index);
        $(this).children().first().find("input").attr("name", "concept_name_input" + c_index);
        $(this).children().first().find("button").attr("id", "delete_concept" + c_index);
        $(this).children().first().next().attr("id", "seed" + c_index);
        $(this).children().first().next().children().each(function () {     //遍历seed1下的元素
            var index = $(this).parent().children().index(this) + 1;
            $(this).attr("id", "seed_" + c_index + "_" + index);
            $(this).find("input").attr("name", "concept" + c_index + "_seed" + index);
            $(this).find("button").attr("id", "del_seed_" + c_index + "_" + index);
        });
        $(this).children().first().first("button").attr("id", "delete_concept" + c_index);
        $(this).children().last().first("button").attr("id", "add_seed_" + c_index);
    });
}
function del_con_seed(obj) {
    var thisobj=$(obj);
    var strs=thisobj.attr("id").split("_");
    var con=strs[2];
    var seed=strs[3];
    var parent = $("#seed" + con);
    $("#seed_"+con+"_"+seed).remove();
    record[con]--;
    parent.children().each(function () {      //遍历seed1下的元素
        var index = parent.children().index(this) + 1;
        console.log(con);
        $(this).attr("id", "seed_" + con + "_" + index);
        $(this).find("input").attr("name", "concept" + con + "_seed" + index);
        $(this).find("button").attr("id", "del_seed_" + con + "_" + index);
    });

}
function add_seed(obj){
    var thisobj=$(obj);
    var num=thisobj.attr("id").substr(9);
    $("#seed" + num).append($('<div id="seed_' + num + '_' + (++record[num]) + '" style="margin-top: 20px">\n' +
        '                        <button class="btn btn-secondary" disabled style="position: relative;margin-left: 0">种子词</button>\n'+
        '                        <input type="text" name="concept' + num + '_seed' + record[num] + '" value="" class="uk-input" style="position: relative;width:30%;"/>\n' +
        '                        <button type="button" id="del_seed_' + num + '_' + record[num] + '" onclick="del_con_seed(this)" class="btn btn-danger">删除</button>\n' +
        '                    </div>'));
}


function fill_form(field){
    $.get("modify?field=" + field, function (data, statu) {
        var i,j,k;
        $("input[name='field_name_input']").val(data["field"]);
        for(i in data["field_seed"]) {
            if(data["field_seed"][i]!="")
                $("#field_seed").append($('<li id="field_seed_li' + (i-0+1).toString() + '"><input type="text" class="uk-input" style="position: relative;width:30%" name="field_seed_input' + (i-0+1).toString() + '" value="'+data["field_seed"][i]+'"/><button type="button" class="btn btn-danger" onclick="del(this)" id="delete_seed' + (i-0+1).toString() + '">删除</button></li>'));
        }
        for(j in data["concepts"]){
            $("#concept_container").append($('<div id="add_concept'+(j-0+1).toString()+'">\n' +
                '                        <div>\n' +
                '                            <button class="btn btn-secondary" style="position:relative;margin-left:0px" disabled>概念词</button>\n' +
                '                            <input type="text" class="uk-input" style="position: relative;width:30%;margin-top: 8px" name="concept_name_input'+(j-0+1).toString()+'" value="'+data["concepts"][j]["concept"]+'"/>\n' +
                '                            <button type="button" id="delete_concept'+(j-0+1).toString()+'" onclick="del_concept(this)" class="btn btn-danger" >删除</button>\n' +
                '                        </div>\n' +
                '                        <div id="seed'+(j-0+1).toString()+'">\n' +
                '                            <div id="seed_'+(j-0+1).toString()+'_1">\n' +
                '                                <button class="btn btn-secondary" disabled style="position: relative;margin-left: 0">种子词</button>\n\n' +
                '                                <input class="uk-input" type="text" name="concept'+(j-0+1).toString()+'_seed1" value="'+data["concepts"][j]["seeds"][0]+'" style="position: relative;width:30%;margin-top: 8px"/><span class="star">*</span>\n' +
                '                            </div>\n' +
                '                        </div>\n' +
                '                        <button type="button" class="btn btn-primary" id="add_seed_'+(j-0+1).toString()+'" onclick="add_seed(this)">添加种子词</button>\n' +
                '                    </div>'));
            for (k = 1; k < data["concepts"][j]["seeds"].length; k++) {
                $("#seed"+(j-0+1).toString()).append($('<div id="seed_'+(j-0+1).toString()+'_'+(k-0+1).toString()+'">\n' +
                    '                        <button class="btn btn-secondary" disabled>种子词</button>\n'+
                    '                        <input type="text" class="uk-input" name="concept'+(j-0+1).toString()+'_seed'+(k-0+1).toString()+'" value="'+data["concepts"][j]["seeds"][k]+'" style="position: relative;width:30%;margin-top: 8px"/>\n' +
                    '                        <button type="button" class="btn btn-danger" id="del_seed_'+(j-0+1).toString()+'_'+(k-0+1).toString()+'" onclick="del_con_seed(this)">删除</button>\n' +
                    '                    </div>'));
            }
            record[j - 0 + 1] = data["concepts"][j]["seeds"].length;
        }
    })
}

$(document).ready(function(){
    //如果参数不为空则为修改原有信息，填充表单
    if(GetQueryString("field")!=null){
        $("input[name='field_name_input']").attr("readonly",true);
        fill_form(GetQueryString("field"));
    }

    //添加领域种子词
    $("#add_seed1").click(function(){
        field_seed = $("li[id^='field_seed_li']").length + 1;
        $("#field_seed").append($('<li id="field_seed_li'+field_seed+'"><input type="text" class="uk-input" style="position:relative;width:30%" name="field_seed_input'+field_seed+'" value=""/><button type="button" onclick="del(this)" id="delete_seed'+field_seed+'" class="btn btn-danger">删除</button></li>'));
        field_seed++;
    });
    //添加概念
    $("#add_concept").click(function () {
        concept = $("div[id^='add_concept']").length + 1;
        $("#concept_container").append($('<div id="add_concept'+concept+'">\n' +
            '                        <div>\n' +
            '                            <button class="btn btn-secondary" style="position:relative;margin-left:0px" disabled>概念词</button>\n' +
            '                            <input type="text" name="concept_name_input'+concept+'" class="uk-input" style="position:relative;width:30%;margin-top: 8px" value=""/>\n' +
            '                            <button type="button" id="delete_concept'+concept+'" onclick="del_concept(this)" class="btn btn-danger">删除</button>\n' +
            '                        </div>\n' +
            '                        <div id="seed'+concept+'">\n' +
            '                            <div id="seed_'+concept+'_1">\n' +
            '                                <button class="btn btn-secondary" style="position:relative;margin-left:0px" disabled>种子词</button>\n' +
            '                                <input type="text" class="uk-input" style="position:relative;width:30%;margin-top: 8px" name="concept'+concept+'_seed1" value=""/><span class="star">*</span>\n' +
            '                            </div>\n' +
            '                        </div>\n' +
            '                        <button type="button" id="add_seed_'+concept+'" onclick="add_seed(this)" class="btn btn-primary">添加种子词</button>\n' +
            '                    </div>'));
        record[concept] = 1;
        concept++;
    });
});