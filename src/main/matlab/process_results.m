
for i = 1:6
    subjectResults = results(table2array(results(:,2)) == i,:);
    names = subjectResults(:,1);
    spll =  subjectResults(tblfind(names, 'SPLL'),:);
    kl =  subjectResults(tblfind(names, 'KL'),:);
    hotelling =  subjectResults(tblfind(names, 'Hotelling'),:);
end

