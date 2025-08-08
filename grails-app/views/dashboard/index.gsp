<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Dashboard - KoolPump</title>
    <style>
        .dashboard-container {
            padding: 30px;
            max-width: 1200px;
            margin: 0 auto;
        }
        .welcome-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .stat-card h3 {
            color: #6c757d;
            font-size: 0.9rem;
            margin-bottom: 10px;
            text-transform: uppercase;
        }
        .stat-value {
            font-size: 2rem;
            font-weight: bold;
            color: #333;
        }
        .section-card {
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        .section-title {
            font-size: 1.3rem;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
        }
        .activity-list {
            list-style: none;
            padding: 0;
        }
        .activity-item {
            padding: 10px;
            border-bottom: 1px solid #f0f0f0;
            display: flex;
            justify-content: space-between;
        }
        .activity-item:last-child {
            border-bottom: none;
        }
        .btn-group {
            display: flex;
            gap: 10px;
            margin-top: 20px;
        }
        .btn {
            padding: 10px 20px;
            border-radius: 5px;
            text-decoration: none;
            transition: all 0.3s;
        }
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        .btn-secondary {
            background: #6c757d;
            color: white;
        }
        .btn-danger {
            background: #dc3545;
            color: white;
        }
        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }
        .subscription-status {
            padding: 15px;
            background: #f8f9fa;
            border-radius: 5px;
            margin-bottom: 15px;
        }
        .subscription-badge {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 3px;
            font-size: 0.85rem;
            font-weight: bold;
            margin-left: 10px;
        }
        .badge-trial {
            background: #ffc107;
            color: #000;
        }
        .badge-active {
            background: #28a745;
            color: white;
        }
        .badge-expired {
            background: #dc3545;
            color: white;
        }
        .payment-history {
            width: 100%;
            border-collapse: collapse;
        }
        .payment-history th {
            background: #f8f9fa;
            padding: 10px;
            text-align: left;
            font-weight: 600;
        }
        .payment-history td {
            padding: 10px;
            border-bottom: 1px solid #dee2e6;
        }
        .payment-status {
            padding: 3px 8px;
            border-radius: 3px;
            font-size: 0.85rem;
        }
        .status-success {
            background: #d4edda;
            color: #155724;
        }
        .status-pending {
            background: #fff3cd;
            color: #856404;
        }
        .status-failed {
            background: #f8d7da;
            color: #721c24;
        }
    </style>
</head>
<body>

<div class="dashboard-container">
    <div class="welcome-header">
        <h1>Welcome back, ${user.firstName}!</h1>
        <p>Manage your account, subscription, and explore heat pump data.</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <h3>Account Status</h3>
            <div class="stat-value">
                <g:if test="${user.enabled}">Active</g:if>
                <g:else>Inactive</g:else>
            </div>
        </div>
        <div class="stat-card">
            <h3>Member Since</h3>
            <div class="stat-value">
                <g:formatDate date="${user.dateCreated}" format="MMM yyyy"/>
            </div>
        </div>
        <div class="stat-card">
            <h3>Subscription Plan</h3>
            <div class="stat-value">
                ${subscription?.planName ?: 'Free Trial'}
            </div>
        </div>
        <div class="stat-card">
            <h3>Next Payment</h3>
            <div class="stat-value">
                <g:if test="${subscription?.nextBillingDate}">
                    <g:formatDate date="${subscription.nextBillingDate}" format="dd MMM"/>
                </g:if>
                <g:else>N/A</g:else>
            </div>
        </div>
    </div>

    <div class="section-card">
        <h2 class="section-title">Subscription & Billing</h2>
        
        <div class="subscription-status">
            <strong>Current Plan:</strong> ${subscription?.planName ?: 'Free Trial'}
            <g:if test="${subscription?.status == 'TRIAL'}">
                <span class="subscription-badge badge-trial">TRIAL - ${subscription.trialDaysRemaining} days left</span>
            </g:if>
            <g:elseif test="${subscription?.status == 'ACTIVE'}">
                <span class="subscription-badge badge-active">ACTIVE</span>
            </g:elseif>
            <g:else>
                <span class="subscription-badge badge-expired">EXPIRED</span>
            </g:else>
            
            <div style="margin-top: 10px;">
                <g:if test="${subscription?.status == 'TRIAL'}">
                    <p>Your trial expires on <strong><g:formatDate date="${subscription.trialEndDate}" format="dd MMM yyyy"/></strong></p>
                    <g:link controller="payment" action="choosePlan" class="btn btn-primary">Upgrade Now</g:link>
                </g:if>
                <g:elseif test="${subscription?.status == 'EXPIRED'}">
                    <g:link controller="payment" action="choosePlan" class="btn btn-primary">Reactivate Subscription</g:link>
                </g:elseif>
                <g:else>
                    <g:link controller="payment" action="manageBilling" class="btn btn-secondary">Manage Billing</g:link>
                </g:else>
            </div>
        </div>

        <h3 style="margin-top: 20px;">Recent Payments</h3>
        <g:if test="${payments}">
            <table class="payment-history">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Description</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Invoice</th>
                    </tr>
                </thead>
                <tbody>
                    <g:each in="${payments}" var="payment">
                        <tr>
                            <td><g:formatDate date="${payment.dateCreated}" format="dd MMM yyyy"/></td>
                            <td>${payment.description}</td>
                            <td>€${payment.amount}</td>
                            <td>
                                <span class="payment-status status-${payment.status.toLowerCase()}">
                                    ${payment.status}
                                </span>
                            </td>
                            <td>
                                <g:if test="${payment.invoiceUrl}">
                                    <a href="${payment.invoiceUrl}" target="_blank">Download</a>
                                </g:if>
                            </td>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <p>No payment history available.</p>
        </g:else>
    </div>

    <div class="section-card">
        <h2 class="section-title">Quick Actions</h2>
        <div class="btn-group">
            <g:link controller="dashboard" action="profile" class="btn btn-primary">Edit Profile</g:link>
            <g:link controller="dashboard" action="changePassword" class="btn btn-secondary">Change Password</g:link>
            <g:link controller="payment" action="updatePaymentMethod" class="btn btn-secondary">Update Payment Method</g:link>
            <g:link controller="dashboard" action="revokeTokens" class="btn btn-danger" 
                    onclick="return confirm('Are you sure you want to revoke all tokens?')">Revoke All Tokens</g:link>
        </div>
    </div>

    <div class="section-card">
        <h2 class="section-title">Recent Activity</h2>
        <g:if test="${recentActivity}">
            <ul class="activity-list">
                <g:each in="${recentActivity}" var="activity">
                    <li class="activity-item">
                        <span>${activity.activity.replaceAll('_', ' ').toLowerCase().capitalize()}</span>
                        <span><g:formatDate date="${activity.timestamp}" format="dd MMM yyyy HH:mm"/></span>
                    </li>
                </g:each>
            </ul>
        </g:if>
        <g:else>
            <p>No recent activity to display.</p>
        </g:else>
    </div>
</div>

</body>
</html>