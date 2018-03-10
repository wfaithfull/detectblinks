function [ ind ] = tblfind( tbl, str )
%TBLFIND Summary of this function goes here
%   Detailed explanation goes here

ind = strfind(table2array(tbl),str);
ind = find(~cellfun(@isempty, ind));

end

